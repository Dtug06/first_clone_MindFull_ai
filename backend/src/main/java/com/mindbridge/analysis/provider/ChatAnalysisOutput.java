package com.mindbridge.analysis.provider;

import java.util.List;

/**
 * Output contract for {@link ChatAnalysisProvider#analyze(ChatAnalysisInput)}.
 *
 * <p>Fields represent the <em>model's</em> view of the message. Risk
 * resolution (combining model risk with keyword/regex rules) and the final
 * safety action are the responsibility of the safety module (G4), not
 * the provider.
 *
 * <p>Field set is locked by G3-T02. The downstream schema
 * ({@code docs/02_DATABASE_MVP.md} §5.2 {@code chat_analysis_results})
 * expects more fields (rule_risk_level, final_risk_level); those are
 * filled in by the Safety Resolver — providers MUST NOT compute the
 * final risk level.
 *
 * <p>The JSON contract for this record is captured in
 * {@code docs/schemas/chat_analysis_v1.schema.json} (Draft 07). The
 * taxonomy of enum values is the single source of truth for both
 * backend and frontend (see {@code docs/schemas/chat_analysis_v1.dictionary.md}).
 *
 * <p>The {@code schemaVersion} field is always
 * {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION}; it is added in
 * G3-T02 (was absent in the G3-T01 record). Bumping the constant is
 * a breaking change — see {@link AnalysisSchemaVersion} for the
 * policy.
 *
 * @param topic           dominant topic, must be a {@link Topic} enum
 *                        value. Non-null.
 * @param emotion         dominant emotion, must be an {@link Emotion}
 *                        enum value. Non-null.
 * @param intent          what the user appears to want, must be an
 *                        {@link Intent} enum value. Non-null.
 * @param signals         behaviour-level tags the model wants to
 *                        surface (e.g. {@link Signal#FATIGUE}). Never
 *                        null, may be empty.
 * @param modelRiskLevel  the model's own risk assessment, 1..4.
 *                        1=normal, 2=follow-up, 3=high risk (open
 *                        safety event), 4=emergency (open safety
 *                        event + fixed response).
 * @param confidence      self-reported confidence in
 *                        {@code [0.0, 1.0]}.
 * @param evidenceSpans   pointers into the input message that justify
 *                        the output. May be empty for short or
 *                        generic messages.
 * @param latencyMs       wall-clock time the provider spent producing
 *                        the result, in milliseconds. Always
 *                        {@code >= 0}. For mocked providers this is
 *                        the synthetic delay.
 * @param errorCode       stable code for partial / failed outputs
 *                        (e.g. {@code "MALFORMED_JSON"}).
 *                        {@code null} for successful outputs.
 * @param schemaVersion   schema version of this record. Always
 *                        {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION}.
 */
public record ChatAnalysisOutput(
        Topic topic,
        Emotion emotion,
        Intent intent,
        List<Signal> signals,
        int modelRiskLevel,
        double confidence,
        List<EvidenceSpan> evidenceSpans,
        long latencyMs,
        String errorCode,
        String schemaVersion
) {
    public ChatAnalysisOutput {
        if (topic == null) {
            throw new IllegalArgumentException("topic must not be null");
        }
        if (emotion == null) {
            throw new IllegalArgumentException("emotion must not be null");
        }
        if (intent == null) {
            throw new IllegalArgumentException("intent must not be null");
        }
        if (signals == null) {
            throw new IllegalArgumentException("signals must not be null (use List.of() for empty)");
        }
        signals = List.copyOf(signals);
        if (modelRiskLevel < 1 || modelRiskLevel > 4) {
            throw new IllegalArgumentException(
                    "modelRiskLevel must be in [1, 4] but was " + modelRiskLevel);
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                    "confidence must be in [0.0, 1.0] but was " + confidence);
        }
        if (evidenceSpans == null) {
            throw new IllegalArgumentException(
                    "evidenceSpans must not be null (use List.of() for empty)");
        }
        evidenceSpans = List.copyOf(evidenceSpans);
        if (latencyMs < 0) {
            throw new IllegalArgumentException(
                    "latencyMs must be >= 0 but was " + latencyMs);
        }
        if (schemaVersion == null || schemaVersion.isBlank()) {
            throw new IllegalArgumentException("schemaVersion must not be null or blank");
        }
        if (!AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException(
                    "schemaVersion must be "
                            + AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION
                            + " but was " + schemaVersion);
        }
    }
}
