package com.mindbridge.safety.classifier.config;

import com.mindbridge.safety.classifier.RiskClassifierProvider;
import com.mindbridge.safety.classifier.provider.impl.MockRiskClassifierProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires a {@link RiskClassifierProvider} bean based on the
 * {@code mindbridge.ai.risk-classifier.provider} property. The MVP
 * ships one implementation:
 *
 * <ul>
 *   <li>{@code mock}  — {@link MockRiskClassifierProvider}, default
 *       for local and test profiles. Deterministic, offline.</li>
 * </ul>
 *
 * <p>A real LLM-based implementation is planned post-MVP. When added,
 * it will be registered as a separate bean keyed on
 * {@code mindbridge.ai.risk-classifier.provider=real}.
 *
 * <p>The {@link ConditionalOnMissingBean} guard means a test or a
 * future profile can register its own {@link RiskClassifierProvider}
 * bean without touching this file.
 *
 * <p><b>Why this is a separate config from
 * {@code com.mindbridge.analysis.config.ChatAnalysisProviderConfig}:</b>
 * the chat analysis pipeline and the risk classifier pipeline are
 * independent layers of the Safety architecture
 * ({@code docs/01_ARCHITECTURE.md} §9). Each has its own provider
 * property and its own mock. Wiring them separately prevents a
 * regression in one from masking or breaking the other.
 */
@Configuration
public class RiskClassifierProviderConfig {

    /**
     * Registers the mock provider when
     * {@code mindbridge.ai.risk-classifier.provider=mock}
     * AND no other {@link RiskClassifierProvider} bean has been
     * declared.
     */
    @Bean
    @ConditionalOnProperty(prefix = "mindbridge.ai.risk-classifier", name = "provider",
            havingValue = "mock", matchIfMissing = true)
    @ConditionalOnMissingBean(RiskClassifierProvider.class)
    public RiskClassifierProvider mockRiskClassifierProvider() {
        return new MockRiskClassifierProvider();
    }
}
