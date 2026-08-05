package com.mindbridge.behavior.feature.job;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mindbridge.feature-aggregation")
public record DailyFeatureAggregationProperties(boolean enabled, int batchSize) {
}
