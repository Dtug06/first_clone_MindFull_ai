package com.mindbridge.checkin.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after an answer is submitted.
 */
public record DailyAnswerResponse(
        UUID id,
        UUID assignmentId,
        Instant answeredAt
) {
}
