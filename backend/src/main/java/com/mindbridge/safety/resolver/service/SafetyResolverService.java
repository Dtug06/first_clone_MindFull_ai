package com.mindbridge.safety.resolver.service;

import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.dto.MatchedRule;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.resolver.RiskStateHistory;
import com.mindbridge.safety.resolver.RiskStateHistoryRepository;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.resolver.dto.ResolverInput;
import com.mindbridge.safety.resolver.exception.SafetyResolverInputException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Combines the keyword/regex pre-filter signal (G3-T08) and the LLM
 * risk classifier signal (G3-T09) into a single final risk level,
 * persists the result in the append-only {@link RiskStateHistory}
 * table, and exposes the latest state for downstream consumers.
 *
 * <p>This is the "Safety Rule Resolver" of docs/01_ARCHITECTURE.md §9
 * and the only component authorised to write to
 * {@code risk_state_history}.
 *
 * <h2>Decision rule (G3-T10 Phase 1, locked)</h2>
 *
 * <pre>
 *   ruleRisk   = preFilter?.preliminaryRisk() ?? 1
 *   modelRisk  = classifier?.riskLevel()      ?? 1
 *   previous   = latestHistory?.riskLevel     ?? null
 *
 *   candidate  = max(ruleRisk, modelRisk)          // max wins (Q2)
 *
 *   if (previous != null && candidate < previous) {
 *       finalRisk = previous                       // no auto-downgrade (Q3)
 *       reasonCodes = [...signals, "MANUAL_REVIEW_REQUIRED"]
 *   } else {
 *       finalRisk = candidate
 *       reasonCodes = [...signals, "MAX_WINS_L{finalRisk}"]
 *   }
 *
 *   persist 1 RiskStateHistory row with the snapshots:
 *     - risk_level         = finalRisk
 *     - model_risk_level   = modelRisk
 *     - rule_risk_level    = ruleRisk
 *     - current_risk_level = previous
 *     - rule_version       = preFilter?.ruleVersion ?? "NONE"
 *     - model_version      = classifier?.providerInfo   (null if no classifier)
 *     - prompt_version     = classifier?.promptVersion  (null if no classifier)
 *     - confidence         = max(confidence of the two signals, or 0.0)
 *     - reason_codes       = the JSONB array above
 *     - occurred_at        = Clock.instant() (UTC)
 * </pre>
 *
 * <p>The decision is <b>deterministic</b> given the same
 * {@code ResolverInput} and the same {@code Clock} — i.e. running
 * {@code resolve(input)} twice in a row against an empty history
 * produces the same {@code finalRiskLevel} and {@code reason}. Two
 * separate calls still produce two rows (append-only) but the
 * decision per call is reproducible.
 *
 * <h2>Scope</h2>
 *
 * <p>The service is intentionally <b>not</b> a REST controller — it
 * has no HTTP layer. The wiring to chat, daily-check-in, exercise
 * submissions and program assessments happens in subsequent tasks
 * (G3-T11 in particular, then G6 for the matching safety gate).
 *
 * <h2>Safety invariants (Phase 3 review)</h2>
 *
 * <ul>
 *   <li>No downgrade: a {@code finalRiskLevel} less than {@code previous}
 *       is impossible. Enforced by the guard above and by tests
 *       {@code resolve_neverDecreasesRiskBelowCurrent} and
 *       {@code resolve_keywordL4_classifierL1_currentL1_returnsL4}.</li>
 *   <li>No {@code log} call writes raw content, tokens, secrets, or
 *       PII — only an INFO log at the user-id granularity when a row
 *       is persisted (no message text, no classifier response).</li>
 *   <li>Failure of the classifier (timeout, malformed output,
 *       unavailable) is converted to {@code modelRisk = 1} (no
 *       signal) and never persists an "L4 from a broken classifier"
 *       row.</li>
 *   <li>Append-only: callers cannot update or delete rows through
 *       this service. The repository does not expose those methods,
 *       and the entity has no setters or {@code @PreUpdate}.</li>
 *   <li>Structured audit trail: every row carries a non-empty
 *       {@code reason_codes} JSONB array (DB-MVP §6.1, docs/04 §7).
 *       The path-code ({@code MAX_WINS_L*} or
 *       {@code MANUAL_REVIEW_REQUIRED}) is always appended so audit
 *       can distinguish max-wins from guarded-downgrade without
 *       re-parsing free text.</li>
 *   <li>Caller-responsibility for classifier failures: if
 *       {@code RiskClassifierProvider.classify(...)} throws
 *       (timeout, malformed output, unavailable), the resolver does
 *       NOT catch — the exception propagates to the caller. This
 *       enforces docs/04 §28 "JSON sai schema không được lưu thành
 *       công": we never silently convert a broken classifier into a
 *       "no signal" row. Callers must wrap the call in try/catch and
 *       translate classifier failures into {@code null} + WARN log
 *       (T11 wiring task). Resolver unit tests verify propagation in
 *       {@code resolve_classifierException_propagates} (see test
 *       class).</li>
 * </ul>
 */
@Service
public class SafetyResolverService {

    private static final Logger log = LoggerFactory.getLogger(SafetyResolverService.class);

    /** Schema version constant for the resolver output (matches
     *  {@link RiskStateHistory#CURRENT_SCHEMA_VERSION}). */
    public static final String SCHEMA_VERSION = "V1";

    /** Rule-version snapshot used when no pre-filter result is
     *  available (e.g. caller passed {@code null} for the pre-filter
     *  signal, or the pre-filter returned a no-signal result). */
    static final String NO_RULE_VERSION = "NONE";

    /** Path-code appended to {@code reason_codes} when the final risk
     *  level equals {@code max(ruleRisk, modelRisk)}. Format includes
     *  the final level so audit can group by level without parsing
     *  the column. */
    static final String PATH_CODE_MAX_WINS_PREFIX = "MAX_WINS_L";

    /** Path-code appended to {@code reason_codes} when the downgrade
     *  guard kept the previous (higher) risk level. Indicates the
     *  decision can only be lowered via MANUAL_REVIEW (G3-T13). */
    static final String PATH_CODE_MANUAL_REVIEW_REQUIRED = "MANUAL_REVIEW_REQUIRED";

    /** Sentinel returned by a "no signal" classifier run. Marked with
     *  the {@code _DEMO} suffix per docs/04 §1 to keep demo codes
     *  obviously non-production. */
    static final String NO_SIGNAL_CODE = "NO_SIGNAL_DEMO";

    /** Code prefix used when only the pre-filter contributed a code
     *  and we surface a rule code snapshot. */
    static final String RULE_CODE_PREFIX = "RULE_";

    private final RiskStateHistoryRepository historyRepository;
    private final Clock clock;

    public SafetyResolverService(
            RiskStateHistoryRepository historyRepository,
            Clock clock) {
        this.historyRepository = historyRepository;
        this.clock = clock;
    }

    /**
     * Resolve one safety decision for one user. Persists a row to
     * {@code risk_state_history} and returns the resulting decision.
     *
     * <p>This method is the only path that inserts rows in the
     * history table.
     *
     * @param input the resolver input. Must not be null and its
     *              {@code userId} + {@code sourceType} are required.
     * @return the {@link ResolverDecision} including the persisted row.
     * @throws SafetyResolverInputException when the input is invalid.
     */
    @Transactional
    public ResolverDecision resolve(ResolverInput input) {
        if (input == null) {
            throw new SafetyResolverInputException("ResolverInput must not be null");
        }
        if (input.userId() == null) {
            throw new SafetyResolverInputException("ResolverInput.userId must not be null");
        }
        if (input.sourceType() == null) {
            throw new SafetyResolverInputException("ResolverInput.sourceType must not be null");
        }

        PreFilterResult preFilter = input.preFilterResult();
        RiskClassifierOutput classifier = input.classifierOutput();

        // Signals — null/missing means "no signal" → treat as risk 1.
        int ruleRisk = preFilter != null ? preFilter.preliminaryRisk() : 1;
        int modelRisk = classifier != null ? classifier.riskLevel() : 1;

        // Previous state — null when this is the user's first resolution.
        Optional<RiskStateHistory> previousRow =
                historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(input.userId());
        Short previousRisk = previousRow.map(r -> Short.valueOf(r.getRiskLevel())).orElse(null);

        // Max wins.
        int candidate = Math.max(ruleRisk, modelRisk);

        // Downgrade guard: keep current if the new signals are lower.
        short finalRisk;
        String pathCode;
        if (previousRisk != null && candidate < previousRisk.intValue()) {
            finalRisk = previousRisk.shortValue();
            pathCode = PATH_CODE_MANUAL_REVIEW_REQUIRED;
        } else {
            finalRisk = (short) candidate;
            pathCode = PATH_CODE_MAX_WINS_PREFIX + finalRisk;
        }

        // Confidence — pick the stronger of the two signals (capped at 1).
        BigDecimal confidence = resolveConfidence(preFilter, classifier);

        // Snapshots from the signals.
        String ruleVersion = preFilter != null ? preFilter.ruleVersion() : NO_RULE_VERSION;
        String modelVersion = classifier != null ? classifier.providerInfo() : null;
        String promptVersion = classifier != null ? classifier.promptVersion() : null;

        // Build the structured reason_codes array (DB-MVP §6.1 + §7):
        //   1. classifier reasonCodes (verbatim) — empty list if no classifier
        //   2. pre-filter rule code(s) — one code per matched rule, or
        //      NO_SIGNAL_DEMO if no rule matched but pre-filter ran
        //   3. path code (MAX_WINS_L* / MANUAL_REVIEW_REQUIRED)
        String[] reasonCodes = buildReasonCodes(
                classifier, preFilter, pathCode);

        RiskStateHistory row = RiskStateHistory.record(
                UUID.randomUUID(),
                input.userId(),
                finalRisk,
                classifier != null ? Short.valueOf((short) modelRisk) : null,
                Short.valueOf((short) ruleRisk),
                previousRisk,
                input.sourceType(),
                input.sourceId(),
                ruleVersion,
                modelVersion,
                promptVersion,
                confidence,
                reasonCodes,
                OffsetDateTime.now(clock));

        RiskStateHistory persisted = historyRepository.save(row);

        log.info("Persisted risk_state_history row id={} userId={} finalRisk=L{} source={} pathCode={}",
                persisted.getId(), persisted.getUserId(),
                persisted.getRiskLevel(), persisted.getSourceType(),
                persisted.getReasonCodes()[persisted.getReasonCodes().length - 1]);

        return new ResolverDecision(
                persisted.getRiskLevel(),
                persisted.getModelRiskLevel(),
                persisted.getRuleRiskLevel(),
                persisted.getCurrentRiskLevel(),
                persisted.getConfidence(),
                persisted.getReasonCodes(),
                persisted);
    }

    /**
     * Read the current risk state for a user. Returns the latest row
     * by {@code occurred_at} (ties broken by {@code id DESC} via the
     * repository method). Empty when the user has no history.
     *
     * <p>This is the read API used by the matching safety gate (G6)
     * and by any audit / support tooling that needs to surface the
     * user's current risk level without going through {@code resolve}
     * again.
     */
    @Transactional(readOnly = true)
    public Optional<RiskStateHistory> getCurrentRiskState(UUID userId) {
        if (userId == null) {
            throw new SafetyResolverInputException("userId must not be null");
        }
        return historyRepository.findFirstByUserIdOrderByOccurredAtDescIdDesc(userId);
    }

    private static BigDecimal resolveConfidence(
            PreFilterResult preFilter, RiskClassifierOutput classifier) {
        BigDecimal c1 = preFilter != null ? BigDecimal.valueOf(preFilter.confidence()) : BigDecimal.ZERO;
        BigDecimal c2 = classifier != null ? BigDecimal.valueOf(classifier.confidence()) : BigDecimal.ZERO;
        BigDecimal picked = c1.max(c2);
        return picked.setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Build the structured {@code reason_codes} array per DB-MVP §6.1
     * and docs/04 §7. Order is stable so audit / log diffs are
     * deterministic:
     *
     * <ol>
     *   <li>Classifier reason codes (verbatim, in input order).
     *       Empty when no classifier was provided.</li>
     *   <li>Pre-filter rule codes (one per matched rule). When the
     *       pre-filter ran but matched nothing, a single
     *       {@link #NO_SIGNAL_CODE} is added so the absence of any
     *       rule is itself auditable.</li>
     *   <li>Path code ({@link #PATH_CODE_MAX_WINS_PREFIX}{@code N} or
     *       {@link #PATH_CODE_MANUAL_REVIEW_REQUIRED}) so audit can
     *       tell max-wins from guarded-downgrade without re-parsing
     *       the row's other columns.</li>
     * </ol>
     *
     * <p>Always returns a non-empty array — the path code alone is
     * sufficient to make the array non-empty when both signals were
     * null.
     */
    static String[] buildReasonCodes(
            RiskClassifierOutput classifier,
            PreFilterResult preFilter,
            String pathCode) {

        List<String> codes = new ArrayList<>();
        if (classifier != null && classifier.reasonCodes() != null) {
            for (String c : classifier.reasonCodes()) {
                if (c != null && !c.isBlank()) {
                    codes.add(c);
                }
            }
        }
        if (preFilter != null) {
            List<MatchedRule> matched = preFilter.matchedRules();
            if (matched != null && !matched.isEmpty()) {
                for (MatchedRule mr : matched) {
                    codes.add(RULE_CODE_PREFIX + mr.ruleCode() + "@" + mr.ruleVersion());
                }
            } else {
                codes.add(NO_SIGNAL_CODE);
            }
        }
        codes.add(pathCode);
        return codes.toArray(new String[0]);
    }
}
