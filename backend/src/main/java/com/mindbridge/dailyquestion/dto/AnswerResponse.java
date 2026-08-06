package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.DailyQuestionAnswer;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a submitted daily question answer.
 *
 * Mirrors DailyAnswerResponse in docs/03_API_CONTRACT.yaml.
 * Returns id, assignmentId, answeredAt (the contract's required fields) plus
 * the echoed value for client confirmation.
 */
public record AnswerResponse(
        UUID id,
        UUID assignmentId,
        AnswerType answerType,
        BigDecimal numericValue,
        String textValue,
        String optionValue,
        Instant answeredAt
) {
    public static AnswerResponse from(DailyQuestionAnswer answer) {
        return new AnswerResponse(
                answer.getId(),
                answer.getAssignment().getId(),
                answer.getAnswerType(),
                answer.getNumericValue(),
                answer.getTextValue(),
                answer.getOptionValue(),
                answer.getAnsweredAt()
        );
    }
}