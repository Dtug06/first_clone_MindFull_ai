package com.mindbridge.behavior.feature.trend.dto;

import java.time.LocalDate;

/**
 * G4-T07 streak information for a single user.
 *
 * @param checkInStreak number of consecutive days (counting back from
 *                      {@code targetDate}) with at least one daily-question
 *                      answer; 0 if {@code targetDate} has no answer
 * @param highStressStreak number of consecutive days with
 *                         {@code stress_score >= HIGH_STRESS_THRESHOLD}; 0
 *                         if {@code targetDate} does not meet threshold
 * @param lastCheckInDate most recent date with a check-in (null if none in
 *                         the streak window)
 * @param lastHighStressDate most recent date with high stress (null if none
 *                           in the streak window)
 * @param streakWindowDays the cap applied to streak computation (default 30)
 */
public record StreakInfo(
        int checkInStreak,
        int highStressStreak,
        LocalDate lastCheckInDate,
        LocalDate lastHighStressDate,
        int streakWindowDays
) {}