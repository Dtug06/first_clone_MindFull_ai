package com.mindbridge.checkin.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Daily check-in question assignment returned by GET /daily-checkins/today.
 */
public record DailyQuestionAssignmentResponse(
        UUID assignmentId,
        String templateCode,
        QuestionType questionType,
        String prompt,
        LocalDate assignedForDate,
        List<DailyQuestionOptionResponse> options,
        boolean answered
) {

    public enum QuestionType {
        SCALE, SINGLE_CHOICE, TEXT, NUMBER
    }
}
