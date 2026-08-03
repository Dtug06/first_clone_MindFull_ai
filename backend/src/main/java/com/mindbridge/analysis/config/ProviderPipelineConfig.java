package com.mindbridge.analysis.config;

import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link ProviderRetryProperties} under
 * {@code mindbridge.ai.pipeline.retry.*} and
 * {@code mindbridge.ai.pipeline.fallback.*} (G3-T07).
 */
@Configuration
@EnableConfigurationProperties(ProviderRetryProperties.class)
public class ProviderPipelineConfig {
}
