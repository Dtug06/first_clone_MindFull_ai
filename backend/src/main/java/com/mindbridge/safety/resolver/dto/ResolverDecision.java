package com.mindbridge.safety.resolver.dto;

import com.mindbridge.safety.resolver.RiskStateHistory;
import java.math.BigDecimal;

/**
 * Output of {@code SafetyResolverService.resolve(...)}. Carries the
 * final resolved risk level, the contributing signals, and the
 * persisted {@link RiskStateHistory} row that this resolution
 * produced.
 *
 * <p>The fields mirror the columns on the history row (audit by
 * construction — see docs/04_SAFETY_AND_CBT_RULES.md §5 "Phải phân
 * biệt model_risk_level, rule_risk_level, final_risk_level" and §7
 * "LLM Safety Output phải là Structured JSON với reasonCodes[]").
 *
 * @param finalRiskLevel   the resolved level (1..4). Equal to
 *                         {@code historyRow.riskLevel}.
 * @param modelRiskLevel   the classifier signal (null if the
 *                         classifier did not contribute).
 * @param ruleRiskLevel    the pre-filter signal (1 if no rule matched).
 * @param previousRiskLevel the user's level before this resolution
 *                          (null for the first resolution).
 * @param confidence       the resolver's overall confidence in
 *                         {@code [0.0, 1.0]}. For now this mirrors the
 *                         stronger of the two signals' confidences
 *                         (the one that drove the max) and falls back
 *                         to 0.0 when neither signal produced any
 *                         risk.
 * @param reasonCodes      structured reason codes per DB-MVP §6.1
 *                         and docs/04 §7. Always non-empty. Codes
 *                         produced by the two signals (classifier
 *                         {@code reasonCodes[]} and pre-filter rule
 *                         code) are combined in front; a path-code
 *                         is appended last so audit can distinguish
 *                         {@code MAX_WINS_L*} from
 *                         {@code MANUAL_REVIEW_REQUIRED}.
 * @param historyRow       the persisted append-only row. Never null.
 */
public record ResolverDecision(
        short finalRiskLevel,
        Short modelRiskLevel,
        Short ruleRiskLevel,
        Short previousRiskLevel,
        BigDecimal confidence,
        String[] reasonCodes,
        RiskStateHistory historyRow
) {
}
