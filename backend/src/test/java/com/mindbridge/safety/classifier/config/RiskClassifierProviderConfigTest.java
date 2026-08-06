package com.mindbridge.safety.classifier.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.safety.classifier.RiskClassifierProvider;
import com.mindbridge.safety.classifier.provider.impl.MockRiskClassifierProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Unit test for {@link RiskClassifierProviderConfig} using Spring's
 * {@link ApplicationContextRunner}. No Spring context is actually
 * started, so this test runs in milliseconds and does NOT depend on
 * the database or the rest of the application.
 *
 * <p>Two scenarios:
 * <ul>
 *   <li>{@code mindbridge.ai.risk-classifier.provider=mock} (default) →
 *       a {@link MockRiskClassifierProvider} bean is registered.</li>
 *   <li>No {@link RiskClassifierProvider} bean declared, no property
 *       set → mock bean still appears thanks to
 *       {@code matchIfMissing=true}.</li>
 * </ul>
 */
@DisplayName("RiskClassifierProviderConfig wiring")
class RiskClassifierProviderConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class))
            .withUserConfiguration(RiskClassifierProviderConfig.class);

    @Test
    @DisplayName("provider=mock → MockRiskClassifierProvider bean registered")
    void mockProviderRegistersBean() {
        runner.withPropertyValues("mindbridge.ai.risk-classifier.provider=mock")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RiskClassifierProvider.class);
                    assertThat(ctx.getBean(RiskClassifierProvider.class))
                            .isInstanceOf(MockRiskClassifierProvider.class);
                });
    }

    @Test
    @DisplayName("property missing → mock bean still registered (matchIfMissing)")
    void defaultIsMock() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(RiskClassifierProvider.class);
            assertThat(ctx.getBean(RiskClassifierProvider.class))
                    .isInstanceOf(MockRiskClassifierProvider.class);
        });
    }

    @Test
    @DisplayName("provider=real → no RiskClassifierProvider bean yet (post-MVP)")
    void realProviderHasNoBeanYet() {
        runner.withPropertyValues("mindbridge.ai.risk-classifier.provider=real")
                .run(ctx -> {
                    // Real impl is a post-MVP task. Until then, the property
                    // explicitly disables mock and no provider is available —
                    // consumers must guard against this.
                    assertThat(ctx).doesNotHaveBean(RiskClassifierProvider.class);
                });
    }
}
