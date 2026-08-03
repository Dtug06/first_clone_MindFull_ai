package com.mindbridge.dailyquestion.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for GET /daily-checkins/history (per-day grouping).
 *
 * One element per local-date with answers submitted on that date.
 * Dates are returned DESC (most recent first).
 */
public record CheckinHistoryResponse(
        LocalDate date,
        String timezone,
        List<AnswerResponse> answers
) {}