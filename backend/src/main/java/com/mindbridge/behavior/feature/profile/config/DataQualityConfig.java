package com.mindbridge.behavior.feature.profile.config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import com.mindbridge.behavior.feature.profile.DataQualityStatus;

/**
 * G4-T11 configuration value object for data quality thresholds.
 *
 * <p>Determines the {@link DataQualityStatus} of a user behavior profile
 * from its explicit coverage and inferred confidence.
 *
 * <p><b>All thresholds are {@code TODO_EXPERT_REVIEW}</b> per
 * {@code docs/analysis/FEATURE_DICTIONARY_v1.md \u00a710.1}
 * ({@code DATA_QUALITY_THRESHOLDS}) and
 * {@code docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md \u00a713.1}.
 * Defaults are intentionally {@code null} to <b>fail fast</b> — callers
 * (scheduled job, CLI runner, integration test fixture) MUST inject approved
 * values before invoking the evaluator.  A {@code null} threshold causes a
 * {@code NullPointerException} on evaluation, never a silent fallback to a
 * fabricated number (rule 00: "khong tu bia clinical threshold").
 *
 * <p>Validation rules:
 * <ul>
 *   <li>{@code minCoverageForLow}: if non-null, must be in [0, 1].</li>
 *   <li>{@code minCoverageForSufficient}: if non-null, must be in [0, 1]
 *       and be &ge; {@code minCoverageForLow}.</li>
 *   <li>{@code minConfidence}: if non-null, must be in [0, 1].</li>
 * </ul>
 *
 * <p>Decision logic:
 * <pre>
 * if (coverage &ge; minCoverageForSufficient &amp;&amp; confidence &ge; minConfidence) SUFFICIENT
 * else if (coverage &ge; minCoverageForLow)                        LOW
 * else                                                                INSUFFICIENT
 * </pre>
 */
public final class DataQualityConfig {

    /**
     * Minimum explicit coverage (fraction of days with explicit data) required
     * for a {@link DataQualityStatus#LOW} profile.
     * Must be &le; {@code minCoverageForSufficient}.
     */
    private final BigDecimal minCoverageForLow;

    /**
     * Minimum explicit coverage required for a
     * {@link DataQualityStatus#SUFFICIENT} profile.
     * Must be &ge; {@code minCoverageForLow}.
     */
    private final BigDecimal minCoverageForSufficient;

    /**
     * Minimum inferred confidence required for a
     * {@link DataQualityStatus#SUFFICIENT} or {@link DataQualityStatus#LOW}
     * profile.  Below this, the status is always
     * {@link DataQualityStatus#INSUFFICIENT} regardless of coverage.
     */
    private final BigDecimal minConfidence;

    private DataQualityConfig(BigDecimal minCoverageForLow,
                             BigDecimal minCoverageForSufficient,
                             BigDecimal minConfidence) {
        this.minCoverageForLow = minCoverageForLow;
        this.minCoverageForSufficient = minCoverageForSufficient;
        this.minConfidence = minConfidence;
    }

    /**
     * Build a config with explicit threshold values.
     * Validation is performed before construction — an
     * {@link IllegalArgumentException} is thrown on invalid values.
     *
     * @param minCoverageForLow        coverage &ge; 0 and &le; 1
     * @param minCoverageForSufficient coverage &ge; 0 and &le; 1 and
     *                                 &ge; {@code minCoverageForLow}
     * @param minConfidence           confidence &ge; 0 and &le; 1
     * @return a validated config
     * @throws IllegalArgumentException if any value violates the documented bounds
     */
    public static DataQualityConfig of(BigDecimal minCoverageForLow,
                                     BigDecimal minCoverageForSufficient,
                                     BigDecimal minConfidence) {
        validateRatio("minCoverageForLow", minCoverageForLow);
        validateRatio("minConfidence", minConfidence);
        if (minCoverageForSufficient != null) {
            validateRatio("minCoverageForSufficient", minCoverageForSufficient);
            if (minCoverageForLow != null
                    && minCoverageForSufficient.compareTo(minCoverageForLow) < 0) {
                throw new IllegalArgumentException(
                        "minCoverageForSufficient must be >= minCoverageForLow; got "
                                + minCoverageForSufficient + " < " + minCoverageForLow);
            }
        }
        return new DataQualityConfig(
                minCoverageForLow, minCoverageForSufficient, minConfidence);
    }

    /**
     * Returns a config with all thresholds set to {@code null}.
     * Suitable only for <b>unit tests that do NOT exercise status evaluation</b>.
     * Any call to {@link #evaluate(BigDecimal, BigDecimal)} with this
     * instance will throw {@code NullPointerException} — which is the
     * intended fail-fast behaviour for unconfigured production use.
     */
    public static DataQualityConfig defaults() {
        return new DataQualityConfig(null, null, null);
    }

    private static void validateRatio(String name, BigDecimal value) {
        if (value != null
                && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(
                    name + " must be in [0, 1] or null; got " + value);
        }
    }

    /**
     * Evaluate the data quality status from explicit coverage and inferred
     * confidence.
     *
     * @param dataCoverage explicit coverage fraction (0..1), from
     *                    {@code user_behavior_profiles.data_coverage}
     * @param confidence  inferred confidence fraction (0..1), from
     *                    {@code user_behavior_profiles.confidence}
     * @return the evaluated {@link DataQualityStatus}
     * @throws NullPointerException if any threshold is {@code null}
     *                             (fail-fast — thresholds must be injected
     *                             before evaluation)
     */
    public DataQualityStatus evaluate(BigDecimal dataCoverage, BigDecimal confidence) {
        if (confidence.compareTo(minConfidence) < 0) {
            return DataQualityStatus.INSUFFICIENT;
        }
        if (dataCoverage.compareTo(minCoverageForSufficient) >= 0) {
            return DataQualityStatus.SUFFICIENT;
        }
        if (dataCoverage.compareTo(minCoverageForLow) >= 0) {
            return DataQualityStatus.LOW;
        }
        return DataQualityStatus.INSUFFICIENT;
    }

    public BigDecimal getMinCoverageForLow() {
        return minCoverageForLow;
    }

    public BigDecimal getMinCoverageForSufficient() {
        return minCoverageForSufficient;
    }

    public BigDecimal getMinConfidence() {
        return minConfidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DataQualityConfig that)) return false;
        return Objects.equals(minCoverageForLow, that.minCoverageForLow)
                && Objects.equals(minCoverageForSufficient,
                                 that.minCoverageForSufficient)
                && Objects.equals(minConfidence, that.minConfidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minCoverageForLow,
                           minCoverageForSufficient, minConfidence);
    }

    @Override
    public String toString() {
        return "DataQualityConfig{"
                + "minCoverageForLow=" + minCoverageForLow
                + ", minCoverageForSufficient=" + minCoverageForSufficient
                + ", minConfidence=" + minConfidence
                + "}";
    }
}
