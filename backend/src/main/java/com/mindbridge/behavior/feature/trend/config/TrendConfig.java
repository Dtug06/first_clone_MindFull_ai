package com.mindbridge.behavior.feature.trend.config;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * G4-T07 configuration value object for trend and streak thresholds.
 *
 * <p>All three thresholds are {@code TODO_EXPERT_REVIEW} in
 * {@code docs/analysis/FEATURE_DICTIONARY_v1.md §10.1} +
 * {@code docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md §13.1}:
 * <ul>
 *   <li>{@code MIN_TREND_COVERAGE} — minimum coverage required in both the
 *       recent 7d window and the prior 7d window before declaring a trend.
 *       Below this, the entry is {@code UNKNOWN} with reason
 *       {@code INSUFFICIENT_*_COVERAGE}.</li>
 *   <li>{@code TREND_DELTA_THRESHOLD} — {@code |delta_pct|} at or below this
 *       value is treated as {@code STABLE} (no meaningful change).</li>
 *   <li>{@code HIGH_STRESS_THRESHOLD} — {@code stress_score >= this} on the
 *       0-1 normalized scale is counted toward the high-stress streak.</li>
 * </ul>
 *
 * <p>Defaults are intentionally {@code null} to fail fast — callers MUST
 * inject values from a properties source (T12 controller, scheduled job,
 * integration test fixture) before invoking the calculator. Failing fast
 * prevents silently inventing values (rule 00: "khong tu bia clinical
 * threshold").
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code MIN_TREND_COVERAGE}: if non-null, must be in [0, 1].</li>
 *   <li>{@code TREND_DELTA_THRESHOLD}: if non-null, must be in [0, 10]
 *       (delta_pct is a ratio but experts may express as 0.10 = 10%
 *       or 10 = 10x; bounded to [0, 10] for sanity).</li>
 *   <li>{@code HIGH_STRESS_THRESHOLD}: if non-null, must be in [0, 1].</li>
 * </ul>
 */
public final class TrendConfig {

    private final BigDecimal minTrendCoverage;
    private final BigDecimal trendDeltaThreshold;
    private final BigDecimal highStressThreshold;

    private TrendConfig(BigDecimal minTrendCoverage,
                        BigDecimal trendDeltaThreshold,
                        BigDecimal highStressThreshold) {
        this.minTrendCoverage = minTrendCoverage;
        this.trendDeltaThreshold = trendDeltaThreshold;
        this.highStressThreshold = highStressThreshold;
    }

    public static TrendConfig of(BigDecimal minTrendCoverage,
                                 BigDecimal trendDeltaThreshold,
                                 BigDecimal highStressThreshold) {
        validateRatio("minTrendCoverage", minTrendCoverage);
        validateDelta("trendDeltaThreshold", trendDeltaThreshold);
        validateRatio("highStressThreshold", highStressThreshold);
        return new TrendConfig(minTrendCoverage, trendDeltaThreshold, highStressThreshold);
    }

    /**
     * Returns a config with all three thresholds set to {@code null}.
     * Use only for unit tests that do NOT exercise trend/streak logic.
     */
    public static TrendConfig defaults() {
        return new TrendConfig(null, null, null);
    }

    private static void validateRatio(String name, BigDecimal value) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 1] or null; got " + value);
        }
    }

    private static void validateDelta(String name, BigDecimal value) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.TEN) > 0)) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 10] or null; got " + value);
        }
    }

    public BigDecimal getMinTrendCoverage() {
        return minTrendCoverage;
    }

    public BigDecimal getTrendDeltaThreshold() {
        return trendDeltaThreshold;
    }

    public BigDecimal getHighStressThreshold() {
        return highStressThreshold;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TrendConfig that)) return false;
        return Objects.equals(this.minTrendCoverage, that.minTrendCoverage)
                && Objects.equals(this.trendDeltaThreshold, that.trendDeltaThreshold)
                && Objects.equals(this.highStressThreshold, that.highStressThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minTrendCoverage, trendDeltaThreshold, highStressThreshold);
    }

    @Override
    public String toString() {
        return "TrendConfig{minTrendCoverage=" + minTrendCoverage
                + ", trendDeltaThreshold=" + trendDeltaThreshold
                + ", highStressThreshold=" + highStressThreshold + "}";
    }
}