package com.mindbridge.analysis.config;

import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.impl.MockChatAnalysisProvider;
import com.mindbridge.analysis.provider.impl.RealLlmChatAnalysisProvider;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import com.mindbridge.analysis.provider.validation.ChatAnalysisSchemaValidator;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Wires a {@link ChatAnalysisProvider} bean based on the
 * {@code mindbridge.ai.provider} property. The MVP ships two
 * implementations:
 *
 * <ul>
 *   <li>{@code mock}  — {@link MockChatAnalysisProvider}, default for
 *       local and test profiles. Deterministic, offline.</li>
 *   <li>{@code real}  — {@link RealLlmChatAnalysisProvider.Wired},
 *       the hosted LLM provider. This implementation is gated on the
 *       {@code mindbridge.ai.provider} property (set to {@code real}).
 *       Construction validates all required {@code mindbridge.ai.real.*}
 *       properties and the presence of the API key env var; any
 *       failure surfaces as a bean-factory {@code IllegalStateException}
 *       that prevents the application from starting — fail-secure per
 *       Phase 1 Q5.</li>
 * </ul>
 *
 * <p>The {@link ConditionalOnMissingBean} guard means a test or a
 * future profile can register its own {@link ChatAnalysisProvider} bean
 * without touching this file.
 */
@Configuration
public class ChatAnalysisProviderConfig {

    /**
     * Registers the mock provider when {@code mindbridge.ai.provider=mock}
     * AND no other {@link ChatAnalysisProvider} bean has been declared.
     */
    @Bean
    @ConditionalOnProperty(prefix = "mindbridge.ai", name = "provider",
            havingValue = "mock", matchIfMissing = true)
    @ConditionalOnMissingBean(ChatAnalysisProvider.class)
    public ChatAnalysisProvider mockChatAnalysisProvider() {
        return new MockChatAnalysisProvider();
    }

    /**
     * Registers the real LLM provider when {@code mindbridge.ai.provider=real}.
     * Construction validates all configuration and the presence of the
     * API key env var; failure surfaces as a bean-factory
     * {@code IllegalStateException} that prevents the application from
     * starting — fail-secure per Phase 1 Q5.
     *
     * <p>The bean receives (G3-T07): the schema validator and the shared
     * retry properties. The fallback bean (if any) is intentionally NOT
     * autowired here to avoid a circular reference: this factory method
     * IS a {@link ChatAnalysisProvider}, so Spring would otherwise detect
     * a "currently in creation" dependency on itself. The
     * {@link RealLlmChatAnalysisProvider#Wired} constructor reads
     * {@code mindbridge.ai.provider.fallback.enabled} from the
     * environment and treats the absence of an injected fallback as
     * "fallback disabled". Future T11+ wiring can introduce a separate
     * bean lookup that does not cycle back to this factory.
     */
    @Bean
    @ConditionalOnProperty(prefix = "mindbridge.ai", name = "provider",
            havingValue = "real")
    @ConditionalOnMissingBean(ChatAnalysisProvider.class)
    public ChatAnalysisProvider realLlmChatAnalysisProvider(
            HttpClient httpClient,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            ChatAnalysisSchemaValidator validator,
            ProviderRetryProperties retryProperties,
            Environment environment) {
        return new RealLlmChatAnalysisProvider.Wired(
                httpClient, objectMapper, validator, retryProperties, environment);
    }

    /**
     * Shared {@link HttpClient} used by the real provider. The real
     * provider applies a per-request timeout on the {@code HttpRequest}
     * itself, so a short connect timeout at the client level is
     * sufficient here.
     */
    @Bean
    public HttpClient chatAnalysisHttpClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
}
