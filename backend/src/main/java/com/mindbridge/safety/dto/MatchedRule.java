package com.mindbridge.safety.dto;

import com.mindbridge.analysis.provider.EvidenceSpan;
import com.mindbridge.safety.domain.MatchType;
import java.util.List;

/**
 * One rule that matched against the input content, together with the
 * evidence spans (offsets only — never raw text) and the rule's
 * preliminary risk contribution.
 *
 * <p>This record is an INPUT to the Safety Resolver (G3-T10). The
 * resolver combines these signals with the LLM's
 * {@code model_risk_level} and the current user risk state to compute
 * {@code final_risk_level}. Nothing in the pre-filter decides the
 * final risk level on its own — see
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.2 and §6.
 *
 * @param ruleCode        stable code identifying the rule family, e.g.
 *                        {@code "SAFETY_SELF_HARM_V1"}. Never {@code null}
 *                        or blank.
 * @param ruleVersion     exact version of the rule that matched, e.g.
 *                        {@code "v1"}. Combined with {@code ruleCode} this
 *                        forms the audit key required by
 *                        {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.2.
 * @param matchType       whether the match came from a literal keyword
 *                        substring or a compiled regex.
 * @param preliminaryRisk the rule's risk contribution, in {@code [1, 4]}.
 *                        The pre-filter does NOT take the max across
 *                        rules — that aggregation is the resolver's job
 *                        (see {@code SafetyPreFilterService}). This field
 *                        is the rule's own declared risk.
 * @param evidenceSpans   character offsets into the normalized input
 *                        where the match was found. Always at least one
 *                        span. Raw text is replaced by SHA-256 hex hashes
 *                        (see {@link EvidenceSpan}).
 */
public record MatchedRule(
        String ruleCode,
        String ruleVersion,
        MatchType matchType,
        int preliminaryRisk,
        List<EvidenceSpan> evidenceSpans
) {
    public MatchedRule {
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new IllegalArgumentException("ruleCode must not be null or blank");
        }
        if (ruleVersion == null || ruleVersion.isBlank()) {
            throw new IllegalArgumentException("ruleVersion must not be null or blank");
        }
        if (matchType == null) {
            throw new IllegalArgumentException("matchType must not be null");
        }
        if (preliminaryRisk < 1 || preliminaryRisk > 4) {
            throw new IllegalArgumentException(
                    "preliminaryRisk must be in [1, 4] but was " + preliminaryRisk);
        }
        evidenceSpans = evidenceSpans == null ? List.of() : List.copyOf(evidenceSpans);
    }
}
