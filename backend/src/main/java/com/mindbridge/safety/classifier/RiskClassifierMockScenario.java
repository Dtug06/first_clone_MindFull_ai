package com.mindbridge.safety.classifier;

import com.mindbridge.analysis.provider.EvidenceSpan;
import java.util.List;

/**
 * The fixed set of scenarios the {@code MockRiskClassifierProvider} can
 * produce. Scenarios are intentionally minimal so the provider stays
 * deterministic — every input maps to exactly one scenario, and every
 * scenario produces a fixed {@link RiskClassifierOutput}.
 *
 * <p>The four risk levels map directly to
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.1–§3.4 and the safety
 * test cases in
 * {@code .cursor/rules/30-database-ai-safety.mdc} §28. {@code TIMEOUT}
 * and {@code MALFORMED_JSON} are the two failure modes the Safety
 * Resolver (G3-T10) must handle without persisting a successful run.
 *
 * <p>Reason codes per scenario are intentionally illustrative — the
 * real taxonomy is an expert-review item (per §7 "Ví dụ schema" which
 * shows only {@code DISTRESS_SIGNAL} and {@code SLEEP_DISRUPTION} as
 * examples; §1 forbids inventing clinical categories). All labels
 * here are {@code DEMO_ONLY}.
 */
public enum RiskClassifierMockScenario {

    /** Model risk 1: normal, no safety follow-up needed. */
    LEVEL_1_NORMAL {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            return new RiskClassifierOutput(
                    1,
                    0.0,
                    List.of(),
                    List.of(),
                    RiskClassifierOutput.DEMO_PROMPT_VERSION,
                    RiskClassifierOutput.CURRENT_SCHEMA_VERSION,
                    RiskClassifierOutput.PROVIDER_MOCK_V1
            );
        }
    },

    /** Model risk 2: follow-up signal, no safety event yet. */
    LEVEL_2_FOLLOWUP {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            return new RiskClassifierOutput(
                    2,
                    0.78,
                    List.of("DISTRESS_SIGNAL_DEMO", "SLEEP_DISRUPTION_DEMO"),
                    List.of(),
                    RiskClassifierOutput.DEMO_PROMPT_VERSION,
                    RiskClassifierOutput.CURRENT_SCHEMA_VERSION,
                    RiskClassifierOutput.PROVIDER_MOCK_V1
            );
        }
    },

    /** Model risk 3: high risk, Safety Resolver opens a safety event. */
    LEVEL_3_HIGH_RISK {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            return new RiskClassifierOutput(
                    3,
                    0.85,
                    List.of("BURNOUT_DEMO", "HOPELESSNESS_DEMO"),
                    List.of(),
                    RiskClassifierOutput.DEMO_PROMPT_VERSION,
                    RiskClassifierOutput.CURRENT_SCHEMA_VERSION,
                    RiskClassifierOutput.PROVIDER_MOCK_V1
            );
        }
    },

    /** Model risk 4: emergency, fixed approved response required. */
    LEVEL_4_EMERGENCY {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            return new RiskClassifierOutput(
                    4,
                    0.95,
                    List.of("SELF_HARM_RISK_DEMO"),
                    List.of(),
                    RiskClassifierOutput.DEMO_PROMPT_VERSION,
                    RiskClassifierOutput.CURRENT_SCHEMA_VERSION,
                    RiskClassifierOutput.PROVIDER_MOCK_V1
            );
        }
    },

    /**
     * Provider took too long. Implementation throws
     * {@code RiskClassifierTimeoutException}; no output is produced.
     */
    TIMEOUT {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            throw new IllegalStateException(
                    "TIMEOUT scenario never produces an output");
        }
    },

    /**
     * Provider returned a payload that fails schema validation.
     * Implementation throws
     * {@code InvalidRiskClassifierOutputException}.
     */
    MALFORMED_JSON {
        @Override
        public RiskClassifierOutput defaultOutput(long latencyMs) {
            throw new IllegalStateException(
                    "MALFORMED_JSON scenario never produces an output");
        }
    };

    /** Build the deterministic output for this scenario. */
    public abstract RiskClassifierOutput defaultOutput(long latencyMs);
}
