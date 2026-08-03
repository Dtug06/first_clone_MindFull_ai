package com.mindbridge.analysis.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.provider.impl.MockChatAnalysisProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link MockChatAnalysisProvider}. Pure JUnit 5 + AssertJ —
 * does NOT boot a Spring context.
 *
 * <p>Goals:
 * <ul>
 *   <li>Verify that each DEMO keyword maps to the correct scenario.</li>
 *   <li>Verify that the two sentinels (TIMEOUT, MALFORMED_JSON) throw the
 *       right exception instead of returning an output.</li>
 *   <li>Verify the default fallback when no keyword matches.</li>
 *   <li>Verify the {@code force-scenario} override forces any scenario.</li>
 *   <li>Verify the output shape: risk level range, confidence range,
 *       latency non-negative, signals list non-null.</li>
 * </ul>
 */
@DisplayName("MockChatAnalysisProvider")
class MockChatAnalysisProviderTest {

    private MockChatAnalysisProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockChatAnalysisProvider();
    }

    private ChatAnalysisInput input(String content) {
        return new ChatAnalysisInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                "vi-VN"
        );
    }

    // --- Keyword → scenario mapping ---

    @Nested
    @DisplayName("Keyword → scenario resolution")
    class KeywordResolution {

        @Test
        @DisplayName("Vietnamese self-harm phrase → LEVEL_4_EMERGENCY")
        void level4() {
            ChatAnalysisOutput out = provider.analyze(input("Tôi cảm thấy tuyệt vọng"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
            assertThat(out.signals()).contains(Signal.SELF_HARM_RISK);
        }

        @Test
        @DisplayName("Burnout phrase → LEVEL_3_HIGH_RISK")
        void level3() {
            ChatAnalysisOutput out = provider.analyze(input("Tuần này tôi kiệt sức"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Anxiety phrase → LEVEL_2_FOLLOWUP")
        void level2() {
            ChatAnalysisOutput out = provider.analyze(input("Tôi lo lắng quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Positive phrase → LEVEL_1_NORMAL")
        void level1() {
            ChatAnalysisOutput out = provider.analyze(input("Hôm nay tôi thấy tốt"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("English keyword matches case-insensitively")
        void englishKeyword() {
            ChatAnalysisOutput out = provider.analyze(input("I feel BURNOUT this week"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("No keyword match → defaults to LEVEL_1_NORMAL")
        void defaultsToLevel1() {
            ChatAnalysisOutput out = provider.analyze(input("Xin chào, tôi muốn trò chuyện"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Most severe keyword wins when content matches multiple levels")
        void severityOrdering() {
            // "tuyệt vọng" (LEVEL_4) and "lo lắng" (LEVEL_2) both match.
            ChatAnalysisOutput out = provider.analyze(input("Tôi lo lắng và tuyệt vọng"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
        }
    }

    // --- Sentinels: force a scenario explicitly ---

    @Nested
    @DisplayName("Sentinel strings")
    class Sentinels {

        @Test
        @DisplayName("force:TIMEOUT → ProviderTimeoutException")
        void forceTimeout() {
            assertThatThrownBy(() -> provider.analyze(input("force:TIMEOUT")))
                    .isInstanceOf(ProviderTimeoutException.class);
        }

        @Test
        @DisplayName("force:MALFORMED_JSON → InvalidAnalysisOutputException")
        void forceMalformed() {
            assertThatThrownBy(() -> provider.analyze(input("force:MALFORMED_JSON")))
                    .isInstanceOf(InvalidAnalysisOutputException.class);
        }

        @Test
        @DisplayName("force:LEVEL_3_HIGH_RISK with trailing text still maps correctly")
        void forceWithTrailingText() {
            ChatAnalysisOutput out = provider.analyze(
                    input("force:LEVEL_3_HIGH_RISK: trailing text"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Unknown sentinel falls back to keyword resolution")
        void unknownSentinelFallsThrough() {
            ChatAnalysisOutput out = provider.analyze(input("force:UNKNOWN_SCENARIO here"));
            // falls through to LEVEL_1_NORMAL (no keyword matches)
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }
    }

    // --- Force scenario via constructor ---

    @Nested
    @DisplayName("Constructor force-scenario override")
    class ForceScenarioOverride {

        @Test
        @DisplayName("forced LEVEL_4_EMERGENCY returns level 4 even with normal content")
        void forcedLevel4() {
            MockChatAnalysisProvider forced =
                    new MockChatAnalysisProvider("LEVEL_4_EMERGENCY");
            ChatAnalysisOutput out = forced.analyze(input("Xin chào"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("forced TIMEOUT always throws")
        void forcedTimeout() {
            MockChatAnalysisProvider forced = new MockChatAnalysisProvider("TIMEOUT");
            assertThatThrownBy(() -> forced.analyze(input("anything")))
                    .isInstanceOf(ProviderTimeoutException.class);
        }

        @Test
        @DisplayName("blank force value means use keyword resolution")
        void blankForceMeansKeyword() {
            MockChatAnalysisProvider forced = new MockChatAnalysisProvider("");
            ChatAnalysisOutput out = forced.analyze(input("Hôm nay tôi thấy vui"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("unknown force value throws at construction time")
        void unknownForceValue() {
            assertThatThrownBy(() -> new MockChatAnalysisProvider("NOT_A_SCENARIO"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // --- Output shape invariants ---

    @Nested
    @DisplayName("Output shape invariants")
    class OutputShape {

        @Test
        @DisplayName("modelRiskLevel is always in [1, 4]")
        void riskLevelInRange() {
            for (String content : new String[]{
                    "Hôm nay tôi vui", "Tôi lo lắng", "Burnout", "tuyệt vọng"}) {
                ChatAnalysisOutput out = provider.analyze(input(content));
                assertThat(out.modelRiskLevel()).isBetween(1, 4);
            }
        }

        @Test
        @DisplayName("confidence is always in [0.0, 1.0]")
        void confidenceInRange() {
            ChatAnalysisOutput out = provider.analyze(input("Hôm nay tôi thấy tốt"));
            assertThat(out.confidence()).isBetween(0.0, 1.0);
        }

        @Test
        @DisplayName("latencyMs is always >= 0")
        void latencyNonNegative() {
            ChatAnalysisOutput out = provider.analyze(input("Xin chào"));
            assertThat(out.latencyMs()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("topic, emotion, intent are non-null")
        void requiredLabelsNonBlank() {
            ChatAnalysisOutput out = provider.analyze(input("Xin chào"));
            assertThat(out.topic()).isNotNull();
            assertThat(out.emotion()).isNotNull();
            assertThat(out.intent()).isNotNull();
        }

        @Test
        @DisplayName("signals is never null (may be empty)")
        void signalsNotNull() {
            ChatAnalysisOutput out = provider.analyze(input("Xin chào"));
            assertThat(out.signals()).isNotNull();
        }

        @Test
        @DisplayName("errorCode is null for success scenarios")
        void errorCodeNullOnSuccess() {
            ChatAnalysisOutput out = provider.analyze(input("Hôm nay tôi thấy tốt"));
            assertThat(out.errorCode()).isNull();
        }
    }

    // --- Exception contract ---

    @Nested
    @DisplayName("Exception contract")
    class ExceptionContract {

        @Test
        @DisplayName("ProviderTimeoutException carries AI_PROVIDER_TIMEOUT code")
        void timeoutExceptionCode() {
            try {
                provider.analyze(input("force:TIMEOUT"));
            } catch (ProviderTimeoutException ex) {
                assertThat(ex.getCode().getCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
                return;
            }
            throw new AssertionError("Expected ProviderTimeoutException");
        }

        @Test
        @DisplayName("InvalidAnalysisOutputException carries AI_ANALYSIS_OUTPUT_INVALID code")
        void malformedExceptionCode() {
            try {
                provider.analyze(input("force:MALFORMED_JSON"));
            } catch (InvalidAnalysisOutputException ex) {
                assertThat(ex.getCode().getCode()).isEqualTo("AI_ANALYSIS_OUTPUT_INVALID");
                return;
            }
            throw new AssertionError("Expected InvalidAnalysisOutputException");
        }
    }

    // --- Determinism ---

    @Nested
    @DisplayName("Determinism")
    class Determinism {

        @Test
        @DisplayName("Same input produces same output across calls")
        void sameInputSameOutput() {
            ChatAnalysisInput in = input("Hôm nay tôi thấy tốt");
            ChatAnalysisOutput a = provider.analyze(in);
            ChatAnalysisOutput b = provider.analyze(in);
            assertThat(a.topic()).isEqualTo(b.topic());
            assertThat(a.modelRiskLevel()).isEqualTo(b.modelRiskLevel());
            assertThat(a.confidence()).isEqualTo(b.confidence());
            assertThat(a.signals()).isEqualTo(b.signals());
        }
    }

    // --- G3-T03: anchored to the fixed test cases in
    // docs/prompts/chat_analysis_test_cases.md. These tests simply
    // assert that the mock provider matches the table in that file.
    // When the real LLM provider (G3-T06) lands, the same cases
    // become the regression baseline — see the doc's "Future
    // production test set" section.

    @Nested
    @DisplayName("G3-T03 test cases (docs/prompts/chat_analysis_test_cases.md)")
    class TestCasesFromG3T03 {

        // --- Level 1 — Normal (cases #1, #2, #3) ---

        @Test
        @DisplayName("Case #1: 'hôm nay tôi thấy tốt' → L1")
        void case1_homNayToiThayTot() {
            ChatAnalysisOutput out = provider.analyze(input("hôm nay tôi thấy tốt"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Case #2: 'hôm nay trời đẹp' → L1 (no keyword fallback)")
        void case2_homNayTroiDep() {
            ChatAnalysisOutput out = provider.analyze(input("hôm nay trời đẹp"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Case #3: 'vui quá' → L1 (vui keyword)")
        void case3_vuiQua() {
            ChatAnalysisOutput out = provider.analyze(input("vui quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        // --- Level 1 — Ambiguous / low confidence (cases #4, #5) ---

        @Test
        @DisplayName("Case #4: 'tôi không biết nữa' → L1 (no signal)")
        void case4_khongBietNua() {
            ChatAnalysisOutput out = provider.analyze(input("tôi không biết nữa"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("Case #5: 'thôi, kệ' → L1 (short dismissive)")
        void case5_thoiKe() {
            ChatAnalysisOutput out = provider.analyze(input("thôi, kệ"));
            assertThat(out.modelRiskLevel()).isEqualTo(1);
        }

        // --- Level 2 — Follow-up (cases #6, #7, #8) ---

        @Test
        @DisplayName("Case #6: 'deadline dí quá, tôi lo lắng' → L2 + BURNOUT signal")
        void case6_deadlineDiQua() {
            ChatAnalysisOutput out = provider.analyze(
                    input("Hôm nay deadline dí quá, tôi lo lắng."));
            assertThat(out.modelRiskLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Case #7: 'áp lực công việc nhiều quá' → L2")
        void case7_apLucCongViec() {
            ChatAnalysisOutput out = provider.analyze(input("áp lực công việc nhiều quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("Case #8: 'áp lực deadline nhiều quá' → L2")
        void case8_apLucDeadline() {
            ChatAnalysisOutput out = provider.analyze(
                    input("Dạo này áp lực deadline nhiều quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(2);
        }

        // --- Level 3 — High Risk (cases #9, #10, #11, #12) ---

        @Test
        @DisplayName("Case #9: 'tôi mệt mỏi quá' → L3")
        void case9_metMoiQua() {
            ChatAnalysisOutput out = provider.analyze(input("tôi mệt mỏi quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Case #10: 'tôi kiệt sức rồi' → L3")
        void case10_kietSuc() {
            ChatAnalysisOutput out = provider.analyze(input("tôi kiệt sức rồi"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Case #11: 'tôi không chịu nổi nữa' → L3")
        void case11_khongChiuNoi() {
            ChatAnalysisOutput out = provider.analyze(input("tôi không chịu nổi nữa"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("Case #12: 'tôi burnout rồi' → L3")
        void case12_burnout() {
            ChatAnalysisOutput out = provider.analyze(input("tôi burnout rồi"));
            assertThat(out.modelRiskLevel()).isEqualTo(3);
        }

        // --- Level 4 — Emergency (cases #13, #14, #15) ---

        @Test
        @DisplayName("Case #13: 'tôi không muốn sống nữa' → L4 + SELF_HARM_RISK signal")
        void case13_khongMuonSong() {
            ChatAnalysisOutput out = provider.analyze(input("tôi không muốn sống nữa"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
            assertThat(out.signals()).contains(Signal.SELF_HARM_RISK);
        }

        @Test
        @DisplayName("Case #14: 'tôi tuyệt vọng quá' → L4")
        void case14_tuyetVong() {
            ChatAnalysisOutput out = provider.analyze(input("tôi tuyệt vọng quá"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("Case #15: 'tôi muốn kết thúc tất cả' → L4")
        void case15_ketThucTatCa() {
            ChatAnalysisOutput out = provider.analyze(input("tôi muốn kết thúc tất cả"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
        }

        // --- Severity tie-breaking (case #16) ---

        @Test
        @DisplayName("Case #16: 'lo lắng và không muốn sống' → L4 (most severe wins)")
        void case16_tieBreak() {
            ChatAnalysisOutput out = provider.analyze(
                    input("tôi lo lắng và không muốn sống"));
            assertThat(out.modelRiskLevel()).isEqualTo(4);
        }

        // --- Failure scenarios (cases #17, #18) ---

        @Test
        @DisplayName("Case #17: 'force:TIMEOUT' → ProviderTimeoutException")
        void case17_timeout() {
            assertThatThrownBy(() -> provider.analyze(input("force:TIMEOUT")))
                    .isInstanceOf(ProviderTimeoutException.class);
        }

        @Test
        @DisplayName("Case #18: 'force:MALFORMED_JSON' → InvalidAnalysisOutputException")
        void case18_malformed() {
            assertThatThrownBy(() -> provider.analyze(input("force:MALFORMED_JSON")))
                    .isInstanceOf(InvalidAnalysisOutputException.class);
        }
    }
}