package com.mindbridge.checkin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for POST /daily-checkins/{assignmentId}/answer.
 */
public record DailyAnswerRequest(
        @NotNull(message = "Answer type is required")
        AnswerType answerType,

        Double numericValue,
        String textValue,
        String optionValue
) {

    public enum AnswerType {
        NUMERIC, TEXT, OPTION
    }
}
