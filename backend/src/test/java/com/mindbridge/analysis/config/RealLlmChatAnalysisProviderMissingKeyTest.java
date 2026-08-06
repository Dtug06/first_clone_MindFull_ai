package com.mindbridge.analysis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import com.mindbridge.analysis.provider.validation.ChatAnalysisSchemaValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Verifies fail-secure behaviour of
 * {@link RealLlmChatAnalysisProvider.Wired} when the API key env var
 * is unset (G3-T06 Phase 1 Q5).
 *
 * <p>Updated for G3-T07: the bean factory now requires
 * {@link ChatAnalysisSchemaValidator} and
 * {@link ProviderRetryProperties} as direct dependencies, so this
 * test registers both explicitly via {@code withUserConfiguration}.
 *
 * <p>G3-T07 contract clarification: the constructor no longer
 * dereferences the API key env var. Validation of the env var
 * NAME happens at construction time (fail-fast on blank name); the
 * actual env var VALUE is dereferenced lazily at request time so a
 * transient secret-rotation gap does not crash the application on
 * startup. The downstream request fails with
 * {@code ProviderUnavailableException} (mapped to 502 by the global
 * handler) when the env var is unset.
 */
@DisplayName("RealLlmChatAnalysisProvider fail-secure on missing API key")
class RealLlmChatAnalysisProviderMissingKeyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class))
            .withUserConfiguration(
                    ChatAnalysisProviderConfig.class,
                    ProviderPipelineConfig.class,
                    ChatAnalysisSchemaValidatorConfig.class);

    @Test
    @DisplayName("Construction succeeds when env-var name is set, even if env-var value is unset")
    void blankApiKey_constructionSucceeds_requestFailsSecure() {
        String unusedVar = "MINDBRIDGE_AI_REAL_API_KEY_MISSING_FOR_TEST_" + System.nanoTime();
        runner
                .withPropertyValues(
                        "mindbridge.ai.provider=real",
                        "mindbridge.ai.real.api-key-env-var=" + unusedVar,
                        "mindbridge.ai.real.model=gpt-4o-mini",
                        "mindbridge.ai.real.max-retries=0",
                        "mindbridge.ai.real.request-timeout-ms=20000")
                .run(ctx -> {
                    // G3-T07 contract: construction succeeds when the env-var
                    // name is non-blank. The actual env-var value is
                    // dereferenced lazily inside httpPost(), so a missing
                    // value surfaces as ProviderUnavailableException at
                    // request time, not at startup. This protects the app
                    // from crash-looping during a secret-rotation gap.
                    assertThat(ctx).hasNotFailed();
                    assertThat(ctx).hasSingleBean(ChatAnalysisProvider.class);

                    // Now exercise the provider path: it must short-circuit
                    // with ProviderUnavailableException naming the unset env
                    // var, never echoing any key value.
                    ChatAnalysisProvider provider = ctx.getBean(ChatAnalysisProvider.class);
                    org.assertj.core.api.Assertions.assertThatThrownBy(
                                    () -> provider.analyze(buildInput()))
                            .isInstanceOf(com.mindbridge.analysis.exception.ProviderUnavailableException.class)
                            .hasMessageContaining(unusedVar)
                            .hasMessageNotContaining("sk-")
                            .hasMessageNotContaining("Bearer");
                });
    }

    @Test
    @DisplayName("Bean startup fails when api-key-env-var PROPERTY is blank")
    void blankApiKeyEnvVarProperty_failsFast() {
        // Even with the G3-T07 lazy env-var dereference, the env-var
        // NAME property itself is still validated at construction time
        // (rule "API key trống → app không crash" — the *name* being
        // blank would silently disable auth, which is a security hole).
        runner
                .withPropertyValues(
                        "mindbridge.ai.provider=real",
                        "mindbridge.ai.real.api-key-env-var=",
                        "mindbridge.ai.real.model=gpt-4o-mini",
                        "mindbridge.ai.real.max-retries=0",
                        "mindbridge.ai.real.request-timeout-ms=20000")
                .run(ctx -> {
                    assertThat(ctx).hasFailed();
                    Throwable failure = ctx.getStartupFailure();
                    // Fail-secure: no leaked real keys, even in startup failure.
                    assertThat(failure.getMessage()).doesNotContain("sk-");
                    assertThat(failure.getMessage()).doesNotContain("Bearer");
                });
    }

    private com.mindbridge.analysis.provider.ChatAnalysisInput buildInput() {
        return new com.mindbridge.analysis.provider.ChatAnalysisInput(
                java.util.UUID.randomUUID(),
                java.util.UUID.randomUUID(),
                "fixture content for missing-key test",
                "vi-VN");
    }

    /**
     * Tiny config that exposes the schema validator as a bean.
     * The validator is annotated {@link org.springframework.stereotype.Component}
     * so component scanning picks it up in production, but
     * {@link ApplicationContextRunner} does NOT do component scanning
     * by default - we must register it explicitly.
     */
    static class ChatAnalysisSchemaValidatorConfig {
        @org.springframework.context.annotation.Bean
        public ChatAnalysisSchemaValidator chatAnalysisSchemaValidator() {
            return new ChatAnalysisSchemaValidator();
        }
    }
}
