package com.mindbridge.behavior.feature.job.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mindbridge.feature-aggregation.run")
public record DailyFeatureAggregationCliProperties(Boolean enabled, String target) {}
