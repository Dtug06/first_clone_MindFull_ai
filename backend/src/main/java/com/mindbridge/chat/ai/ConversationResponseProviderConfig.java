package com.mindbridge.chat.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/** Selects the mock or real conversational response provider. */
@Configuration
public class ConversationResponseProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "mindbridge.ai.response", name = "provider",
            havingValue = "mock", matchIfMissing = true)
    @ConditionalOnMissingBean(ConversationResponseProvider.class)
    public ConversationResponseProvider mockConversationResponseProvider() {
        return new MockConversationResponseProvider();
    }

    @Bean
    @ConditionalOnProperty(prefix = "mindbridge.ai.response", name = "provider",
            havingValue = "real")
    @ConditionalOnMissingBean(ConversationResponseProvider.class)
    public ConversationResponseProvider realConversationResponseProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            Environment environment) {
        return new RealConversationResponseProvider(
                httpClient,
                objectMapper,
                environment.getProperty(
                        "mindbridge.ai.response.api-base-url",
                        environment.getProperty(
                                "mindbridge.ai.real.api-base-url", "https://api.openai.com/v1")),
                environment.getProperty(
                        "mindbridge.ai.real.api-key-env-var", "MINDBRIDGE_AI_REAL_API_KEY"),
                environment.getProperty(
                        "mindbridge.ai.response.model", "gpt-5.5"),
                environment.getProperty(
                        "mindbridge.ai.response.max-output-tokens", Integer.class, 600),
                environment.getProperty(
                        "mindbridge.ai.response.reasoning-effort", "low"),
                environment.getProperty(
                        "mindbridge.ai.response.request-timeout-ms", Long.class, 20000L));
    }
}
