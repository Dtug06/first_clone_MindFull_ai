package com.mindbridge.behavior.feature.trend.dto;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * G4-T07 result returned by {@code TrendCalculator.calculateTrendForUser}.
 *
 * <p>Designed for the {@code GET /api/v1/behavior/profile} endpoint's
 * {@code trendSummary} field (per
 * {@code docs/03_API_CONTRACT.yaml UserBehaviorProfileResponse.trendSummary}
 * - schema is intentionally {@code object + additionalProperties: string};
 * T12 will wrap this record into the API DTO).
 *
 * @param userId the user the trend was calculated for
 * @param targetDate the {@code targetDate} passed to the calculator
 * @param zoneId the user timezone used for local-date boundaries
 * @param entries per-feature trend (8 entries for the 8 MVP features,
 *                including {@code exercise_completion} as
 *                {@code UNKNOWN + NOT_APPLICABLE})
 * @param streakInfo the streak snapshot at {@code targetDate}
 * @param dataQuality placeholder string for T11 alignment. Always
 *                    {@code "TODO_T11_ALIGNED"} until G4-T11 defines
 *                    {@code DATA_QUALITY_THRESHOLDS}. NEVER null.
 * @param calculationVersion always {@code "trend_v1"}; bumped to
 *                           {@code "trend_v2"} etc. if the formula changes
 */
public record TrendSummary(
        UUID userId,
        LocalDate targetDate,
        ZoneId zoneId,
        List<TrendEntry> entries,
        StreakInfo streakInfo,
        String dataQuality,
        String calculationVersion
) {
    public static final String CALCULATION_VERSION = "trend_v1";
    public static final String DATA_QUALITY_PLACEHOLDER = "TODO_T11_ALIGNED";
}