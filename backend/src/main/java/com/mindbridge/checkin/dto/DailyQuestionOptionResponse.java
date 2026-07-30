package com.mindbridge.checkin.dto;

/**
 * Option for single-choice daily questions.
 */
public record DailyQuestionOptionResponse(
        String value,
        String label,
        int orderIndex
) {
}
