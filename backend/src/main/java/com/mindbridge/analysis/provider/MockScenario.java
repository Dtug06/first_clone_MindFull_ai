package com.mindbridge.analysis.provider;

import java.util.List;

/**
 * The fixed set of scenarios the {@link MockChatAnalysisProvider} can
 * produce. Scenarios are intentionally minimal so the provider stays
 * deterministic — every input maps to exactly one scenario, and every
 * scenario produces a fixed {@link ChatAnalysisOutput}.
 *
 * <p>The four risk levels map directly to the Safety resolver expectations
 * in {@code docs/02_DATABASE_MVP.md} §6 and the safety test cases in
 * {@code .cursor/rules/30-database-ai-safety.mdc} §28. {@code TIMEOUT}
 * and {@code MALFORMED_JSON} are the two failure modes that G3-T07 must
 * handle without persisting a successful run.
 */
public enum MockScenario {

    /** Model risk 1: normal, no safety follow-up needed. */
    LEVEL_1_NORMAL {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            return new ChatAnalysisOutput(
                    Topic.WORK_STRESS,
                    Emotion.NEUTRAL,
                    Intent.VENT,
                    List.of(),
                    1,
                    0.72,
                    List.of(),
                    latencyMs,
                    null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION
            );
        }
    },

    /** Model risk 2: follow-up signal, no safety event. */
    LEVEL_2_FOLLOWUP {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            return new ChatAnalysisOutput(
                    Topic.WORK_STRESS,
                    Emotion.ANXIOUS,
                    Intent.VENT,
                    List.of(Signal.FATIGUE),
                    2,
                    0.78,
                    List.of(),
                    latencyMs,
                    null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION
            );
        }
    },

    /** Model risk 3: high risk, Safety Resolver opens a safety event. */
    LEVEL_3_HIGH_RISK {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            return new ChatAnalysisOutput(
                    Topic.WORK_STRESS,
                    Emotion.OVERWHELMED,
                    Intent.SUPPORT,
                    List.of(Signal.FATIGUE, Signal.HOPELESSNESS),
                    3,
                    0.85,
                    List.of(),
                    latencyMs,
                    null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION
            );
        }
    },

    /** Model risk 4: emergency, fixed approved response required. */
    LEVEL_4_EMERGENCY {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            return new ChatAnalysisOutput(
                    Topic.HEALTH,
                    Emotion.DISTRESS,
                    Intent.SUPPORT,
                    List.of(Signal.SELF_HARM_RISK),
                    4,
                    0.95,
                    List.of(),
                    latencyMs,
                    null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION
            );
        }
    },

    /**
     * Provider took too long. Implementation throws
     * {@code ProviderTimeoutException}; no output is produced.
     */
    TIMEOUT {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            throw new IllegalStateException(
                    "TIMEOUT scenario never produces an output");
        }
    },

    /**
     * Provider returned a payload that fails schema validation.
     * Implementation throws {@code InvalidAnalysisOutputException}.
     */
    MALFORMED_JSON {
        @Override
        public ChatAnalysisOutput defaultOutput(long latencyMs) {
            throw new IllegalStateException(
                    "MALFORMED_JSON scenario never produces an output");
        }
    };

    /** Build the deterministic output for this scenario with a given latency. */
    public abstract ChatAnalysisOutput defaultOutput(long latencyMs);
}