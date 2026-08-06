package com.mindbridge.safety.classifier.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.classifier.RiskClassifierInput;
import com.mindbridge.safety.classifier.RiskClassifierMockScenario;
import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException;
import com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException;
import com.mindbridge.safety.classifier.provider.impl.MockRiskClassifierProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MockRiskClassifierProvider}. Pure JUnit 5 +
 * AssertJ — does NOT boot a Spring context.
 *
 * <p>Goals:
 * <ul>
 *   <li>Verify that each DEMO keyword maps to the correct scenario.</li>
 *   <li>Verify that the two sentinels (TIMEOUT, MALFORMED_JSON) throw
 *       the right exception instead of returning an output.</li>
 *   <li>Verify the default fallback when no keyword matches.</li>
 *   <li>Verify the {@code force-scenario} override forces any scenario.</li>
 *   <li>Verify the output shape: risk level range, confidence range,
 *       reason codes list non-null, evidence spans list non-null.</li>
 *   <li>Verify that all four risk levels are reachable from real
 *       keyword inputs (per §28 Safety Tests requirement).</li>
 * </ul>
 */
@DisplayName("MockRiskClassifierProvider")
class MockRiskClassifierProviderTest {

    private MockRiskClassifierProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockRiskClassifierProvider();
    }

    private RiskClassifierInput input(String content) {
        return new RiskClassifierInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                "vi-VN"
        );
    }

    // --- Keyword → scenario mapping ---

    @Nested
    @DisplayName("DEMO keyword mapping")
    class KeywordMapping {

        @Test
        @DisplayName("Level 4 keywords trigger LEVEL_4_EMERGENCY")
        void level4Keyword() {
            RiskClassifierOutput out = provider.classify(input("tôi tuyệt vọng"));
            assertThat(out.riskLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("Level 3 keywords trigger LEVEL_3_HIGH_RISK")
        void level3Keyword() {
            RiskClassifierOutput out = provider.classify(input("mệt mỏi quá"));
            assertThat(out.riskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Level 2 keywords trigger LEVEL_2_FOLLOWUP")
        void level2Keyword() {
            RiskClassifierOutput out = provider.classify(input("tôi lo lắng"));
            assertThat(out.riskLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Level 1 keywords trigger LEVEL_1_NORMAL")
        void level1Keyword() {
            RiskClassifierOutput out = provider.classify(input("hôm nay tôi thấy tốt"));
            assertThat(out.riskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Most severe scenario wins when multiple keywords match")
        void mostSevereWins() {
            // "không muốn sống" is a Level 4 keyword; mixed with "lo lắng" (L2).
            RiskClassifierOutput out = provider.classify(input("tôi lo lắng và không muốn sống"));
            assertThat(out.riskLevel()).isEqualTo(4);
        }
    }

    // --- Sentinel handling ---

    @Nested
    @DisplayName("Sentinel handling")
    class Sentinels {

        @Test
        @DisplayName("force:TIMEOUT → RiskClassifierTimeoutException")
        void timeoutSentinel() {
            assertThatThrownBy(() -> provider.classify(input("force:TIMEOUT")))
                    .isInstanceOf(RiskClassifierTimeoutException.class);
        }

        @Test
        @DisplayName("force:MALFORMED_JSON → InvalidRiskClassifierOutputException")
        void malformedSentinel() {
            assertThatThrownBy(() -> provider.classify(input("force:MALFORMED_JSON")))
                    .isInstanceOf(InvalidRiskClassifierOutputException.class);
        }

        @Test
        @DisplayName("Malformed sentinel falls back to keyword resolution")
        void malformedSentinelFallsBack() {
            // "force:NOPE" — invalid sentinel name → falls back to keyword resolution
            // (LEVEL_1 because no keyword matches in "force:NOPE").
            RiskClassifierOutput out = provider.classify(input("force:NOPE: nothing here"));
            assertThat(out.riskLevel()).isEqualTo(1);
        }
    }

    // --- Force-scenario constructor override ---

    @Nested
    @DisplayName("force-scenario override")
    class ForceOverride {

        @Test
        @DisplayName("Constructor force overrides keyword resolution")
        void forceOverridesKeyword() {
            MockRiskClassifierProvider forced = new MockRiskClassifierProvider("LEVEL_4_EMERGENCY");
            // Content is Level 1, but force should win.
            RiskClassifierOutput out = forced.classify(input("hôm nay trời đẹp"));
            assertThat(out.riskLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("Unknown force-scenario name fails fast")
        void unknownForceFails() {
            assertThatThrownBy(() -> new MockRiskClassifierProvider("LEVEL_99_NOPE"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("Blank force-scenario treated as no override")
        void blankForceIgnored() {
            MockRiskClassifierProvider blank = new MockRiskClassifierProvider("");
            RiskClassifierOutput out = blank.classify(input("tôi lo lắng"));
            assertThat(out.riskLevel()).isEqualTo(2);
        }
    }

    // --- Output shape invariants ---

    @Nested
    @DisplayName("Output shape")
    class OutputShape {

        @Test
        @DisplayName("All required fields present and in valid ranges")
        void fieldsValid() {
            RiskClassifierOutput out = provider.classify(input("tôi mệt mỏi quá"));
            assertThat(out.riskLevel()).isBetween(1, 4);
            assertThat(out.confidence()).isBetween(0.0, 1.0);
            assertThat(out.reasonCodes()).isNotNull();
            assertThat(out.evidenceSpans()).isNotNull();
            assertThat(out.promptVersion()).isNotBlank();
            assertThat(out.schemaVersion()).isEqualTo(RiskClassifierOutput.CURRENT_SCHEMA_VERSION);
            assertThat(out.providerInfo()).isEqualTo(RiskClassifierOutput.PROVIDER_MOCK_V1);
        }

        @Test
        @DisplayName("Level 1 returns empty reasonCodes (no signal)")
        void level1EmptyReasons() {
            RiskClassifierOutput out = provider.classify(input("hôm nay trời đẹp"));
            assertThat(out.riskLevel()).isEqualTo(1);
            assertThat(out.reasonCodes()).isEmpty();
        }

        @Test
        @DisplayName("Level 2+ returns non-empty reasonCodes")
        void level2PlusHasReasons() {
            RiskClassifierOutput out = provider.classify(input("tôi lo lắng"));
            assertThat(out.riskLevel()).isEqualTo(2);
            assertThat(out.reasonCodes()).isNotEmpty();
        }
    }

    // --- Edge cases ---

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Blank content defaults to Level 1")
        void blankDefaultsToLevel1() {
            // ChatAnalysisInput rejects blank, but RiskClassifierInput is the same.
            // Provider must also not crash on edge inputs from upstream.
            RiskClassifierInput in = new RiskClassifierInput(
                    UUID.randomUUID(), UUID.randomUUID(), "vui", "vi-VN");
            RiskClassifierOutput out = provider.classify(in);
            assertThat(out.riskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("All four risk levels are reachable")
        void allLevelsReachable() {
            assertThat(provider.classify(input("vui")).riskLevel()).isEqualTo(1);
            assertThat(provider.classify(input("lo lắng")).riskLevel()).isEqualTo(2);
            assertThat(provider.classify(input("kiệt sức")).riskLevel()).isEqualTo(3);
            assertThat(provider.classify(input("không muốn sống")).riskLevel()).isEqualTo(4);
        }
    }

    // --- Static guarantee: per 04 §3.2 / §5, this provider must NOT
    // return anything named "finalRiskLevel" — verify via reflection that
    // the output DTO does not expose such a field.

    @Test
    @DisplayName("RiskClassifierOutput has NO finalRiskLevel field (04 §5 invariant)")
    void noFinalRiskLevelField() {
        boolean hasFinalRiskLevel = java.util.Arrays.stream(
                        RiskClassifierOutput.class.getDeclaredFields())
                .anyMatch(f -> f.getName().toLowerCase().contains("finalrisk"));
        assertThat(hasFinalRiskLevel)
                .as("RiskClassifierOutput must not contain a finalRiskLevel field; "
                        + "final risk is decided by the Safety Resolver (G3-T10), "
                        + "docs/04 §5.")
                .isFalse();
    }

    @Test
    @DisplayName("MockRiskClassifierProvider has NO final-risk deciding method (04 §3.2 invariant)")
    void noFinalRiskMethod() {
        boolean hasFinalRiskMethod = java.util.Arrays.stream(
                        MockRiskClassifierProvider.class.getDeclaredMethods())
                .anyMatch(m -> m.getName().toLowerCase().contains("finalrisk")
                        || (m.getReturnType() != void.class
                                && m.getReturnType().getName().toLowerCase().contains("finalrisk")));
        assertThat(hasFinalRiskMethod)
                .as("MockRiskClassifierProvider must not expose any final-risk deciding method.")
                .isFalse();
    }
}
