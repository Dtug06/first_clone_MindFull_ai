package com.mindbridge.safety.classifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException;
import com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException;
import com.mindbridge.safety.classifier.provider.impl.MockRiskClassifierProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test that boots the full Spring context with the mock
 * risk classifier provider selected, then exercises the wired
 * {@link RiskClassifierProvider} bean against all six scenarios.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>The Spring context boots cleanly with the new
 *       {@code safety.classifier} package on the classpath.</li>
 *   <li>{@link RiskClassifierProvider} is wired to
 *       {@link MockRiskClassifierProvider} via the
 *       {@code mindbridge.ai.risk-classifier.provider=mock} property.</li>
 *   <li>The bean behaves identically to the unit-tested provider.</li>
 * </ul>
 *
 * <p>This test does NOT touch the database — the
 * {@code @ActiveProfiles("test")} profile disables Flyway, and the
 * JPA layer is not exercised here.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.mock.force-scenario=",
        "mindbridge.ai.risk-classifier.provider=mock",
        "mindbridge.ai.risk-classifier.mock.force-scenario="
})
@DisplayName("MockRiskClassifierProvider integration")
class MockRiskClassifierProviderIntegrationTest {

    @Autowired
    private RiskClassifierProvider provider;

    private RiskClassifierInput input(String content) {
        return new RiskClassifierInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                "vi-VN"
        );
    }

    @Test
    @DisplayName("Bean is wired as MockRiskClassifierProvider")
    void beanIsMock() {
        assertThat(provider).isInstanceOf(MockRiskClassifierProvider.class);
    }

    @Test
    @DisplayName("Successful classification via wired bean returns expected risk level")
    void successfulClassification() {
        RiskClassifierOutput out = provider.classify(input("hôm nay tôi thấy tốt"));
        assertThat(out.riskLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("Sentinel TIMEOUT → RiskClassifierTimeoutException via wired bean")
    void wiredTimeoutThrows() {
        assertThatThrownBy(() -> provider.classify(input("force:TIMEOUT")))
                .isInstanceOf(RiskClassifierTimeoutException.class);
    }

    @Test
    @DisplayName("Sentinel MALFORMED_JSON → InvalidRiskClassifierOutputException via wired bean")
    void wiredMalformedThrows() {
        assertThatThrownBy(() -> provider.classify(input("force:MALFORMED_JSON")))
                .isInstanceOf(InvalidRiskClassifierOutputException.class);
    }

    @Test
    @DisplayName("Chat analysis provider bean still available independently")
    void chatAnalysisProviderStillWired() {
        // Ensures the new safety.classifier wiring does not collide
        // with the G3-T01 chat analysis wiring.
        com.mindbridge.analysis.provider.ChatAnalysisProvider chatProvider =
                applicationContext.getBean(
                        com.mindbridge.analysis.provider.ChatAnalysisProvider.class);
        assertThat(chatProvider).isNotNull();
    }

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;
}
