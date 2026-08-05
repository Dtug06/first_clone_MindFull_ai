package com.mindbridge.behavior.feature.trend.dto;

/**
 * G4-T07 reason enum attached to every {@link TrendEntry}.
 *
 * <p>{@link #SUFFICIENT_DATA} is the only reason that permits a non-UNKNOWN
 * direction. All other reasons force {@link TrendDirection#UNKNOWN} per the
 * task spec \u00a73 ("khong tu bia clinical threshold" + "never default to
 * STABLE when coverage is low").
 */
public enum TrendReason {
    /** Both windows meet MIN_TREND_COVERAGE; UP/DOWN/STABLE permitted. */
    SUFFICIENT_DATA,
    /** Recent 7d window coverage is below MIN_TREND_COVERAGE. */
    INSUFFICIENT_RECENT_COVERAGE,
    /** Prior 7d window coverage is below MIN_TREND_COVERAGE. */
    INSUFFICIENT_PRIOR_COVERAGE,
    /** No data rows in the recent 7d window (avg = null). */
    NO_RECENT_DATA,
    /** No data rows in the prior 7d window (avg = null OR avg == 0). */
    NO_PRIOR_DATA,
    /** Feature is not applicable for this MVP stage (exercise_completion pre-G5). */
    NOT_APPLICABLE
}