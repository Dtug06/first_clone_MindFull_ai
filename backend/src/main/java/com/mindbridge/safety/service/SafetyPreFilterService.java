package com.mindbridge.safety.service;

import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.safety.domain.MatchType;
import com.mindbridge.safety.domain.SafetyKeywordRule;
import com.mindbridge.safety.domain.SafetyRuleStatus;
import com.mindbridge.safety.dto.MatchedRule;
import com.mindbridge.safety.dto.PreFilterInput;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.exception.SafetyPreFilterInputException;
import com.mindbridge.safety.repository.SafetyKeywordRuleRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keyword/regex pre-filter service. Evaluates user content against the
 * set of {@link SafetyRuleStatus#APPROVED} rules stored in
 * {@code safety_keyword_rules} and returns a {@link PreFilterResult}
 * containing matched rules, evidence offsets, the maximum
 * preliminary risk, and a confidence heuristic.
 *
 * <p><b>This service does NOT decide the final risk level.</b> The
 * returned {@code preliminaryRisk} is one of several signals the
 * Safety Resolver (G3-T10) will combine with the LLM's
 * {@code model_risk_level} and the user's current risk state. The
 * absence of any matching rule does NOT mean the content is safe; it
 * only means this particular signal layer produced no hit. See
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.2 and §6.
 *
 * <p><b>What this service will NOT do:</b>
 * <ul>
 *   <li>It will not call any external LLM.</li>
 *   <li>It will not log raw user content.</li>
 *   <li>It will not store anything to the database on its own — the
 *       {@code safety_events} row creation is the consumer's job.</li>
 * </ul>
 *
 * <p>Reads are wrapped in {@code @Transactional(readOnly = true)} so
 * the JDBC connection is held only for the duration of the SELECT —
 * no transaction is opened across any slow work (rule loading is a
 * single indexed SELECT).
 */
@Service
public class SafetyPreFilterService {

    private final SafetyKeywordRuleRepository ruleRepository;
    private final TextNormalizer normalizer;

    public SafetyPreFilterService(
            SafetyKeywordRuleRepository ruleRepository,
            TextNormalizer normalizer) {
        this.ruleRepository = ruleRepository;
        this.normalizer = normalizer;
    }

    /**
     * Evaluate the input content against all {@code APPROVED} rules.
     *
     * @param input the input, with validated fields per
     *              {@link PreFilterInput}'s compact constructor.
     * @return a {@link PreFilterResult} — never {@code null}. When no
     *         rule is loaded or nothing matches, returns
     *         {@link PreFilterResult#empty(String)} with
     *         {@code preliminaryRisk = 1}.
     * @throws SafetyPreFilterInputException only if a caller bypasses
     *         the {@link PreFilterInput} record validation.
     */
    @Transactional(readOnly = true)
    public PreFilterResult evaluate(PreFilterInput input) {
        if (input == null) {
            throw new SafetyPreFilterInputException("input must not be null");
        }

        List<SafetyKeywordRule> approved = ruleRepository.findByStatus(
                SafetyRuleStatus.APPROVED);
        if (approved.isEmpty()) {
            return PreFilterResult.empty(PreFilterResult.PROVIDER_RULE_ENGINE_V1);
        }

        String normalized = normalizer.normalize(input.content());
        String ruleVersionSnapshot = snapshotRuleSetVersion(approved);

        List<MatchedRule> matched = new ArrayList<>();
        int matchedCharCount = 0;

        for (SafetyKeywordRule rule : approved) {
            List<int[]> offsets = findMatchOffsets(rule, normalized);
            if (offsets.isEmpty()) {
                continue;
            }
            List<EvidenceSpan> spans = new ArrayList<>(offsets.size());
            for (int[] offset : offsets) {
                int start = offset[0];
                int end = offset[1];
                String substring = normalized.substring(start, end);
                spans.add(new EvidenceSpan(start, end, sha256Hex(substring)));
                matchedCharCount += (end - start);
            }
            matched.add(new MatchedRule(
                    rule.getCode(),
                    rule.getRuleVersion(),
                    rule.getMatchType(),
                    rule.getPreliminaryRisk(),
                    spans));
        }

        if (matched.isEmpty()) {
            return new PreFilterResult(
                    List.of(),
                    1,
                    ruleVersionSnapshot,
                    0.0,
                    PreFilterResult.PROVIDER_RULE_ENGINE_V1);
        }

        matched.sort(Comparator.comparingInt(MatchedRule::preliminaryRisk).reversed());
        int preliminaryRisk = matched.get(0).preliminaryRisk();
        double confidence = normalized.isEmpty()
                ? 0.0
                : Math.min(1.0, (double) matchedCharCount / normalized.length());

        return new PreFilterResult(
                matched,
                preliminaryRisk,
                ruleVersionSnapshot,
                confidence,
                PreFilterResult.PROVIDER_RULE_ENGINE_V1);
    }

    /**
     * Find all match offsets for a rule against the normalized text.
     * For {@link MatchType#KEYWORD}, returns the start of every
     * occurrence (substring scan, no overlap). For
     * {@link MatchType#REGEX}, compiles the pattern with
     * {@link Pattern#CASE_INSENSITIVE} and returns every match span.
     *
     * @return a list of {@code [start, end)} offset pairs; empty list
     *         when nothing matched.
     */
    private List<int[]> findMatchOffsets(SafetyKeywordRule rule, String normalized) {
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<int[]> offsets = new ArrayList<>();
        if (rule.getMatchType() == MatchType.KEYWORD) {
            String pattern = rule.getPattern().toLowerCase();
            if (pattern.isBlank()) {
                return List.of();
            }
            int from = 0;
            while (from <= normalized.length() - pattern.length()) {
                int idx = normalized.indexOf(pattern, from);
                if (idx < 0) {
                    break;
                }
                offsets.add(new int[]{idx, idx + pattern.length()});
                from = idx + pattern.length();
            }
        } else { // REGEX
            try {
                // NOTE: Java's built-in \b word boundary is ASCII-only
                // — it does NOT fire around Vietnamese (or other
                // non-ASCII) letters like 'ự', 'ữ', 'đ'. Rule authors
                // writing patterns for non-ASCII languages MUST use
                // Unicode-aware lookarounds instead, e.g.
                // "(?<![\\p{L}])(tự tử)(?![\\p{L}])".
                Pattern compiled = Pattern.compile(
                        rule.getPattern(), Pattern.CASE_INSENSITIVE);
                Matcher m = compiled.matcher(normalized);
                while (m.find()) {
                    if (m.start() != m.end()) {
                        offsets.add(new int[]{m.start(), m.end()});
                    }
                }
            } catch (java.util.regex.PatternSyntaxException ex) {
                // A broken regex rule is the operator's fault, not the
                // caller's. Skip this rule but keep evaluating the rest.
                return List.of();
            }
        }
        return offsets;
    }

    /**
     * Build a stable, human-readable snapshot of the rule set used.
     * Format: {@code "code1@v1,code2@v3"} sorted by code. Empty set
     * returns {@code "NONE"}.
     */
    private static String snapshotRuleSetVersion(List<SafetyKeywordRule> rules) {
        if (rules.isEmpty()) {
            return "NONE";
        }
        List<SafetyKeywordRule> sorted = new ArrayList<>(rules);
        sorted.sort(Comparator.comparing(SafetyKeywordRule::getCode));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) sb.append(',');
            SafetyKeywordRule r = sorted.get(i);
            sb.append(r.getCode()).append('@').append(r.getRuleVersion());
        }
        return sb.toString();
    }

    /**
     * Compute the SHA-256 hex digest of a substring, used to fill
     * {@link EvidenceSpan#textHash()}. Never logs the input.
     */
    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(
                    "SHA-256 not available on this JVM", ex);
        }
    }
}
