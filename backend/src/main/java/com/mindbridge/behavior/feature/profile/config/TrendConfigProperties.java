package com.mindbridge.behavior.feature.profile.config;

import com.mindbridge.behavior.feature.trend.config.TrendConfig;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * G4-T12 Spring binding for the three {@link TrendConfig} thresholds.
 *
 * <p>Bound under the {@code mindbridge.trend.*} prefix in
 * {@code application.yml} (placeholder values, marked
 * {@code TODO_EXPERT_REVIEW} per
 * {@code docs/analysis/FEATURE_DICTIONARY_v1.md 뿯½10.1} +
 * {@code docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md 뿯½13.1}).
 * Real values will be injected once the expert review is signed off.
 *
 * <p><b>Scale reminder</b>: {@code HIGH_STRESS_THRESHOLD} is on the
 * <b>normalized 0&ndash;1</b> {@code stress_score} scale (resolved G4-T07
 * Phase 3 review F-1, 2026-08-05). The raw 1&ndash;10 stress value is
 * exposed via {@code stress_raw_value} and is <b>NOT</b> compared against
 * this threshold.
 *
 * <p>{@link #toTrendConfig()} delegates to {@link TrendConfig#of} which
 * validates the bounds (fail-fast on out-of-range or null values when the
 * caller supplies them).
 */
@ConfigurationProperties(prefix = "mindbridge.trend")
public record TrendConfigProperties(
        BigDecimal minTrendCoverage,
        BigDecimal trendDeltaThreshold,
        BigDecimal highStressThreshold) {

    /**
     * Builds a validated {@link TrendConfig}. {@code null} values pass through
     * so the calculator can fail-fast on missing properties at first call.
     */
    public TrendConfig toTrendConfig() {
        return TrendConfig.of(minTrendCoverage, trendDeltaThreshold, highStressThreshold);
    }
}