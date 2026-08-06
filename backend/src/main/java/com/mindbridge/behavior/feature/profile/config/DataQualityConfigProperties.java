package com.mindbridge.behavior.feature.profile.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized G4 data-quality thresholds. Values intentionally have no
 * production defaults because they require expert approval.
 */
@ConfigurationProperties(prefix = "mindbridge.data-quality")
public record DataQualityConfigProperties(
        BigDecimal minCoverageForLow,
        BigDecimal minCoverageForSufficient,
        BigDecimal minConfidence) {

    public DataQualityConfig toConfig() {
        if (minCoverageForLow == null
                || minCoverageForSufficient == null
                || minConfidence == null) {
            throw new IllegalStateException(
                    "TODO_EXPERT_REVIEW: MINDBRIDGE data-quality thresholds are not configured");
        }
        return DataQualityConfig.of(
                minCoverageForLow, minCoverageForSufficient, minConfidence);
    }
}
