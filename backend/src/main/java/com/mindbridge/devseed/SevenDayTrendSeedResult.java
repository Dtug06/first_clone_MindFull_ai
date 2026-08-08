package com.mindbridge.devseed;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Result of the seven-day trend seed execution.
 *
 * @param userEmail           email of the seeded user
 * @param userId             UUID of the seeded user
 * @param day1               first date of the 7-day window (targetDate - 6)
 * @param targetDate         last date of the 7-day window (day 7)
 * @param assignmentsCreated  number of assignment rows created
 * @param answersCreated     number of answer rows created
 * @param profileUpserted    true if the behavior profile was upserted
 * @param dayResults         per-day summary including raw values used
 */
public record SevenDayTrendSeedResult(
        String userEmail,
        UUID userId,
        LocalDate day1,
        LocalDate targetDate,
        int assignmentsCreated,
        int answersCreated,
        boolean profileUpserted,
        List<DayResult> dayResults
) {

    /**
     * Per-day result summary.
     *
     * @param localDate  local date of this day
     * @param values    stress/mood/sleep/energy/open raw values used (truncated to 20 chars for text)
     */
    public record DayResult(LocalDate localDate, List<String> values) {}
}