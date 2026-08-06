package com.mindbridge.behavior.feature.trend;

import com.mindbridge.behavior.feature.trend.config.TrendConfig;
import com.mindbridge.behavior.feature.trend.dto.TrendSummary;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * G4-T07 calculator interface.
 *
 * <p>Pure read-only calculator: computes simple trend UP / DOWN / STABLE
 * (or UNKNOWN with reason) by comparing two 7-day windows (recent vs prior)
 * on the {@code user_daily_features} table, plus check-in streak and
 * high-stress streak.
 *
 * <p>No ML, no LLM, no threshold invented by code (rule 00 +
 * FEATURE_DICTIONARY \u00a710.1 + Pre_G4 contract \u00a713.1).
 *
 * <p>NOTE on naming: "trend" in this calculator refers to a
 * <b>statistical</b> comparison of two consecutive 7-day windows. This is
 * NOT the same as the CBT Program State {@code BASELINE} used by the G5
 * program runtime (see docs/04_SAFETY_AND_CBT_RULES.md \u00a716). The two
 * concepts share the word "baseline" only by coincidence.
 */
public interface TrendCalculator {

    /**
     * @param userId     the user whose trend to compute (must not be null)
     * @param targetDate the last day of the recent 7-day window
     *                   (must not be null; today, or any historical date)
     * @param zoneId     the user timezone for local-date boundary computation
     *                   (must not be null; injected by caller/controller)
     * @param config     the trend + streak configuration (all three
     *                   thresholds must be non-null or {@link IllegalStateException}
     *                   is thrown at validation time)
     * @return trend summary, never null
     */
    TrendSummary calculateTrendForUser(UUID userId, LocalDate targetDate, ZoneId zoneId, TrendConfig config);
}