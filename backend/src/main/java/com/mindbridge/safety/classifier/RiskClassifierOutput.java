package com.mindbridge.safety.classifier;

import com.mindbridge.analysis.provider.EvidenceSpan;
import java.util.List;

/**
 * Output contract for {@link RiskClassifierProvider#classify(RiskClassifierInput)}.
 *
 * <p>Schema follows {@code docs/04_SAFETY_AND_CBT_RULES.md} §7 "LLM Safety
 * Classification" — a structured JSON object with four minimum fields:
 * risk level, confidence, reason codes, and evidence spans. This is the
 * LLM's own view of the message and is intentionally NOT the final risk
 * level; the Safety Resolver (G3-T10) combines this with the keyword/
 * regex signal from {@code SafetyPreFilterService} (G3-T08) and the
 * user's current risk state to produce {@code final_risk_level}.
 *
 * <p>Why this DTO is separate from {@code ChatAnalysisOutput} (G3-T01):
 * the chat analysis output captures the model's full picture of the
 * message (topic, emotion, intent, signals, modelRiskLevel) for the
 * chat consumer; the risk classifier output is a dedicated safety
 * signal — narrower schema, distinct caller (Safety Resolver), and
 * the {@code reasonCodes} field is a safety-specific contract
 * ({@code docs/04_SAFETY_AND_CBT_RULES.md} §7) that does not belong
 * on the chat analysis record.
 *
 * <p>This DTO is one input to the Safety Resolver. It must NOT be
 * persisted as a successful run until the implementation has validated
 * the fields (see {@code RiskClassifierOutput} compact constructor)
 * and the caller has recorded the result in an {@code ai_analysis_runs}
 * row with {@code source_type=RISK_CLASSIFIER} (persistence is a
 * separate task; G3-T09 only produces the signal).
 *
 * @param riskLevel       the model's own risk assessment, in {@code [1, 4]}
 *                        where 1 = normal, 2 = follow-up, 3 = high risk,
 *                        4 = emergency (Level 4 must trigger the fixed
 *                        approved Safety Response — see §3.4 + §12).
 * @param confidence      self-reported confidence in {@code [0.0, 1.0]}.
 *                        Per §7, must lie in this range.
 * @param reasonCodes     short labels for why the model chose this risk
 *                        level, e.g. {@code "DISTRESS_SIGNAL"} or
 *                        {@code "SLEEP_DISRUPTION"} (§7 example list).
 *                        Never null; may be empty. The full taxonomy is
 *                        intentionally not enumerated here — §7 only
 *                        shows examples and §1 forbids inventing
 *                        clinical categories. Production reason code
 *                        taxonomy is an expert-review item (out of T09
 *                        scope).
 * @param evidenceSpans   pointers into the input message that justify
 *                        the classification. May be empty for short
 *                        or generic messages. Reuses
 *                        {@code com.mindbridge.analysis.provider.EvidenceSpan}
 *                        (same {@code start/end/textHash} shape as
 *                        chat analysis, also SHA-256 hex 64-char hashes).
 *                        Per §7, evidence must be tied to a source —
 *                        the caller supplies the source.
 * @param promptVersion   identifies the prompt version that produced
 *                        this output. Stored as a constant for the MVP
 *                        ({@code DEMO_V0}) and replaced when the real
 *                        risk classifier prompt is designed in a later
 *                        task. Per {@code .cursor/rules/30-database-ai-safety.mdc}
 *                        AI Rules, prompt version must be recorded for
 *                        every analysis run.
 * @param schemaVersion   identifies the JSON schema version of this
 *                        output DTO. Bumped only when fields change.
 * @param providerInfo    constant identifying the implementation that
 *                        produced the output, e.g. {@code "MOCK_V1"} or
 *                        {@code "RULE_ENGINE_V1"}. Used for audit
 *                        trails so downstream consumers can attribute
 *                        the source of the signal.
 */
public record RiskClassifierOutput(
        int riskLevel,
        double confidence,
        List<String> reasonCodes,
        List<EvidenceSpan> evidenceSpans,
        String promptVersion,
        String schemaVersion,
        String providerInfo
) {
    /** Current schema version of this DTO. Bump when fields change. */
    public static final String CURRENT_SCHEMA_VERSION = "V1";

    /** Placeholder prompt version for the MVP mock classifier. */
    public static final String DEMO_PROMPT_VERSION = "DEMO_V0";

    /** Provider identifier for the MockRiskClassifierProvider (MVP). */
    public static final String PROVIDER_MOCK_V1 = "MOCK_V1";

    public RiskClassifierOutput {
        if (riskLevel < 1 || riskLevel > 4) {
            throw new IllegalArgumentException(
                    "riskLevel must be in [1, 4] but was " + riskLevel);
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    "confidence must be in [0.0, 1.0] but was " + confidence);
        }
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        evidenceSpans = evidenceSpans == null ? List.of() : List.copyOf(evidenceSpans);
        if (promptVersion == null || promptVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "promptVersion must not be null or blank");
        }
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException(
                    "schemaVersion must not be null or blank");
        }
        if (providerInfo == null || providerInfo.isBlank()) {
            throw new IllegalArgumentException(
                    "providerInfo must not be null or blank");
        }
    }

    /**
     * Convenience: a no-signal result returned by the mock when no rule
     * matches. {@code riskLevel = 1} (no signal = no evidence of risk),
     * {@code confidence = 0.0}, no reason codes, no evidence spans.
     */
    public static RiskClassifierOutput empty(String providerInfo) {
        return new RiskClassifierOutput(
                1, 0.0, List.of(), List.of(),
                DEMO_PROMPT_VERSION, CURRENT_SCHEMA_VERSION, providerInfo);
    }
}
