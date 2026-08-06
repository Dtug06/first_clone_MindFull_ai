package com.mindbridge.safety.dto;

import java.util.List;

/**
 * Output contract for {@code SafetyPreFilterService.evaluate(...)}.
 *
 * <p>This is an INPUT to the Safety Resolver (G3-T10), never a
 * decision. The resolver combines these signals with the LLM's
 * {@code model_risk_level} and the user's current risk state to
 * compute {@code final_risk_level}.
 *
 * <p>Why no {@code finalRiskLevel} field here: pre-filter rule signal
 * alone is insufficient for the final risk decision per
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.2 ("Không được nâng
 * hoặc hạ risk chỉ dựa trên một từ khóa đơn lẻ") and §6
 * ("Keyword không được là cơ sở duy nhất để kết luận Level 4 nếu
 * chưa có rule được phê duyệt").
 *
 * @param matchedRules     rules that produced at least one match,
 *                         ordered by {@code preliminaryRisk} descending
 *                         (most severe first). Empty when no rule
 *                         matched — the resolver interprets that as a
 *                         no-signal pre-filter result.
 * @param preliminaryRisk  the pre-filter's own risk contribution, derived
 *                         as {@code max(matchedRules[*].preliminaryRisk)}
 *                         and clamped to {@code [1, 4]}. When
 *                         {@code matchedRules} is empty, this is
 *                         {@code 1} (no signal). This is NOT the final
 *                         risk level — see class JavaDoc.
 * @param ruleVersion      snapshot of the rule set used for this
 *                         evaluation. Format: comma-separated list of
 *                         {@code code@version} pairs sorted by code,
 *                         e.g. {@code "SAFETY_SELF_HARM@v1,SAFETY_FOLLOWUP@v1"}.
 *                         When no rule is loaded this is
 *                         {@code "NONE"}.
 * @param confidence       heuristic in {@code [0.0, 1.0]} expressing how
 *                         much of the normalized content was covered
 *                         by matched evidence. Implementation-defined
 *                         and meant as a coarse signal, not a calibrated
 *                         probability.
 * @param providerInfo     constant identifying the pre-filter
 *                         implementation, e.g. {@code "RULE_ENGINE_V1"}.
 *                         Used for audit trails so downstream consumers
 *                         can attribute the source of the signal.
 */
public record PreFilterResult(
        List<MatchedRule> matchedRules,
        int preliminaryRisk,
        String ruleVersion,
        double confidence,
        String providerInfo
) {
    public PreFilterResult {
        matchedRules = matchedRules == null ? List.of() : List.copyOf(matchedRules);
        if (preliminaryRisk < 1 || preliminaryRisk > 4) {
            throw new IllegalArgumentException(
                    "preliminaryRisk must be in [1, 4] but was " + preliminaryRisk);
        }
        if (ruleVersion == null || ruleVersion.isBlank()) {
            throw new IllegalArgumentException("ruleVersion must not be null or blank");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    "confidence must be in [0.0, 1.0] but was " + confidence);
        }
        if (providerInfo == null || providerInfo.isBlank()) {
            throw new IllegalArgumentException("providerInfo must not be null or blank");
        }
    }

    /** Convenience: a no-signal result returned when no rule is loaded. */
    public static PreFilterResult empty(String providerInfo) {
        return new PreFilterResult(List.of(), 1, "NONE", 0.0, providerInfo);
    }

    /** Constant provider identifier for v1. */
    public static final String PROVIDER_RULE_ENGINE_V1 = "RULE_ENGINE_V1";
}
