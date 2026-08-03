package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.AnswerType;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;

/**
 * Request body for POST /daily-checkins/{assignmentId}/answer.
 *
 * Mirrors DailyAnswerRequest in docs/03_API_CONTRACT.yaml — uses
 * answerType as discriminator; exactly-one of numericValue/textValue/optionValue
 * must be set, and the type must match.
 *
 * The cross-field validation is enforced in the service layer (with friendly
 * errors) because the @AssertTrue below only catches "zero values set".
 * Type/value matching is checked in service.
 */
public record SubmitAnswerRequest(
        AnswerType answerType,
        BigDecimal numericValue,
        String textValue,
        String optionValue
) {
    /**
     * At least one of numericValue / textValue / optionValue must be set.
     */
    @AssertTrue(message = "Exactly one of numericValue/textValue/optionValue must be provided")
    public boolean isAtLeastOneValueProvided() {
        return numericValue != null
                || (textValue != null && !textValue.isBlank())
                || (optionValue != null && !optionValue.isBlank());
    }
}