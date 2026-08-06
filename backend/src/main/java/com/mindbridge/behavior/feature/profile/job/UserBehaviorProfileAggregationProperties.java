package com.mindbridge.behavior.feature.profile.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * G4-T09 scheduled-job configuration properties.
 *
 * <p>Default schedule = 03:15 UTC (15 minutes after
 * {@code DailyFeatureAggregationJob} at 03:00). This delay is intentional
 * so the daily-feature row is already written when the profile job starts.
 *
 * <p>Disabled by default ({@code mindbridge.profile-aggregation.enabled=false})
 * to keep local development predictable; enabled in {@code application.yml}
 * for prod and {@code application-test.yml} for the integration tests.
 */
@ConfigurationProperties(prefix = "mindbridge.profile-aggregation")
public record UserBehaviorProfileAggregationProperties(
        boolean enabled,
        String scheduleCron,
        int batchSize) {

    public UserBehaviorProfileAggregationProperties {
        if (scheduleCron == null || scheduleCron.isBlank()) {
            scheduleCron = "0 15 3 * * *";
        }
        if (batchSize <= 0) {
            batchSize = 100;
        }
    }
}