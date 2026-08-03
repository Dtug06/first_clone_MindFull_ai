package com.mindbridge.analysis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.impl.MockChatAnalysisProvider;
import com.mindbridge.analysis.provider.impl.RealLlmChatAnalysisProvider;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import com.mindbridge.analysis.provider.validation.ChatAnalysisSchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Unit test for {@link ChatAnalysisProviderConfig} using Spring's
 * {@link ApplicationContextRunner}. No Spring context is actually
 * started, so this test runs in milliseconds and does NOT depend on the
 * database or the rest of the application.
 *
 * <p>Scenarios:
 * <ul>
 *   <li>{@code mindbridge.ai.provider=mock} (default) â†’ a
 *       {@link MockChatAnalysisProvider} bean is registered.</li>
 *   <li>No {@link ChatAnalysisProvider} bean declared, no property set
 *       â†’ mock bean still appears thanks to {@code matchIfMissing=true}.</li>
 *   <li>{@code mindbridge.ai.provider=real} + valid configuration +
 *       the API key env var set (Spring propagates surefire env to
 *       the runner) â†’ {@link RealLlmChatAnalysisProvider.Wired} bean
 *       is registered.</li>
 * </ul>
 *
 * <p>Updated for G3-T07: the {@code realLlmChatAnalysisProvider} bean
 * factory now requires {@link ChatAnalysisSchemaValidator} and
 * {@link ProviderRetryProperties} as direct dependencies. Both are
 * registered via {@code withUserConfiguration} so the runner mirrors
 * the production context.
 */
@DisplayName("ChatAnalysisProviderConfig wiring")
class ChatAnalysisProviderConfigTest {

    /**
     * Tiny config that exposes the schema validator as a bean.
     * {@link ApplicationContextRunner} does NOT do component scanning
     * by default, so explicit registration is required for the
     * real-provider factory method to resolve its dependency.
     */
    static class SchemaValidatorConfig {
        @Bean
        public ChatAnalysisSchemaValidator chatAnalysisSchemaValidator() {
            return new ChatAnalysisSchemaValidator();
        }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration.class,
                    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class))
            .withUserConfiguration(
                    ChatAnalysisProviderConfig.class,
                    SchemaValidatorConfig.class,
                    ProviderPipelineConfig.class);

    @Test
    @DisplayName("provider=mock â†’ MockChatAnalysisProvider bean registered")
    void mockProviderRegistersBean() {
        runner.withPropertyValues("mindbridge.ai.provider=mock")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ChatAnalysisProvider.class);
                    assertThat(ctx.getBean(ChatAnalysisProvider.class))
                            .isInstanceOf(MockChatAnalysisProvider.class);
                });
    }

    @Test
    @DisplayName("property missing â†’ mock bean still registered (matchIfMissing)")
    void defaultIsMock() {
        runner.run(ctx -> {
            assertThat(ctx).hasSingleBean(ChatAnalysisProvider.class);
            assertThat(ctx.getBean(ChatAnalysisProvider.class))
                    .isInstanceOf(MockChatAnalysisProvider.class);
        });
    }

    @Test
    @DisplayName("provider=real â†’ RealLlmChatAnalysisProvider.Wired bean registered")
    void realProviderRegistersBean() {
        // The surefire plugin in pom.xml sets MINDBRIDGE_AI_REAL_API_KEY
        // for tests; the env is inherited by ApplicationContextRunner.
        runner.withPropertyValues(
                        "mindbridge.ai.provider=real",
                        "mindbridge.ai.real.provider-label=openai",
                        "mindbridge.ai.real.model=gpt-4o-mini",
                        "mindbridge.ai.real.api-base-url=https://api.openai.com/v1",
                        "mindbridge.ai.real.api-key-env-var=MINDBRIDGE_AI_REAL_API_KEY",
                        "mindbridge.ai.real.max-tokens=1024",
                        "mindbridge.ai.real.max-retries=1",
                        "mindbridge.ai.real.request-timeout-ms=20000")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(ChatAnalysisProvider.class);
                    assertThat(ctx.getBean(ChatAnalysisProvider.class))
                            .isInstanceOf(RealLlmChatAnalysisProvider.Wired.class);
                });
    }
}
