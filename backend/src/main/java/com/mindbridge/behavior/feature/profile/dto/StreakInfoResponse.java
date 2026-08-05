package com.mindbridge.behavior.feature.profile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindbridge.behavior.feature.trend.dto.StreakInfo;
import java.time.LocalDate;

/**
 * G4-T12 API representation of a {@link StreakInfo}.
 *
 * <p>Streaks are bounded by {@code streakWindowDays} (default 30 per
 * G4-T07). {@code checkInStreak} / {@code highStressStreak} are 0 when
 * the user has not met the threshold at {@code targetDate}.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record StreakInfoResponse(
        int checkInStreak,
        int highStressStreak,
        LocalDate lastCheckInDate,
        LocalDate lastHighStressDate,
        int streakWindowDays) {

    public static StreakInfoResponse from(StreakInfo source) {
        return new StreakInfoResponse(
                source.checkInStreak(),
                source.highStressStreak(),
                source.lastCheckInDate(),
                source.lastHighStressDate(),
                source.streakWindowDays());
    }
}