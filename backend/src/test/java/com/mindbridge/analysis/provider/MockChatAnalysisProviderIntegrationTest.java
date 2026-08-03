package com.mindbridge.analysis.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.provider.impl.MockChatAnalysisProvider;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration test that boots the full Spring context with the
 * mock AI provider selected, then exercises the wired
 * {@link ChatAnalysisProvider} bean against all six scenarios.
 *
 * <p>This test verifies:
 * <ul>
 *   <li>The Spring context boots cleanly with the new {@code analysis}
 *       package on the classpath.</li>
 *   <li>{@link ChatAnalysisProvider} is wired to
 *       {@link MockChatAnalysisProvider} via the
 *       {@code mindbridge.ai.provider=mock} property.</li>
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
        "mindbridge.ai.mock.force-scenario="
})
@DisplayName("MockChatAnalysisProvider integration")
class MockChatAnalysisProviderIntegrationTest {

    @Autowired
    private ChatAnalysisProvider provider;

    private ChatAnalysisInput input(String content) {
        return new ChatAnalysisInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                content,
                "vi-VN"
        );
    }

    @Test
    @DisplayName("Bean is wired as MockChatAnalysisProvider")
    void beanIsMock() {
        assertThat(provider).isInstanceOf(MockChatAnalysisProvider.class);
    }

    @Test
    @DisplayName("Successful analysis via wired bean returns expected risk level")
    void successfulAnalysis() {
        ChatAnalysisOutput out = provider.analyze(input("Hôm nay tôi thấy tốt"));
        assertThat(out.modelRiskLevel()).isEqualTo(1);
        assertThat(out.errorCode()).isNull();
    }

    @Test
    @DisplayName("Sentinel TIMEOUT → ProviderTimeoutException via wired bean")
    void wiredTimeoutThrows() {
        assertThatThrownBy(() -> provider.analyze(input("force:TIMEOUT")))
                .isInstanceOf(ProviderTimeoutException.class);
    }

    @Test
    @DisplayName("Sentinel MALFORMED_JSON → InvalidAnalysisOutputException via wired bean")
    void wiredMalformedThrows() {
        assertThatThrownBy(() -> provider.analyze(input("force:MALFORMED_JSON")))
                .isInstanceOf(InvalidAnalysisOutputException.class);
    }
}