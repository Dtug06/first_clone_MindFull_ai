package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.DailyQuestionOption;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a daily question template (admin view).
 */
public record TemplateResponse(
        UUID id,
        String code,
        int version,
        QuestionType questionType,
        String prompt,
        TemplateStatus status,
        Instant createdAt,
        Instant updatedAt,
        List<OptionResponse> options
) {
    public static TemplateResponse from(DailyQuestionTemplate template) {
        List<OptionResponse> optionResponses = template.getOptions().stream()
                .sorted(Comparator.comparingInt(DailyQuestionOption::getOrderIndex))
                .map(OptionResponse::from)
                .toList();

        return new TemplateResponse(
                template.getId(),
                template.getCode(),
                template.getVersion(),
                template.getQuestionType(),
                template.getPrompt(),
                template.getStatus(),
                template.getCreatedAt(),
                template.getUpdatedAt(),
                optionResponses
        );
    }

    public record OptionResponse(
            UUID id,
            String value,
            String label,
            int orderIndex
    ) {
        public static OptionResponse from(DailyQuestionOption option) {
            return new OptionResponse(
                    option.getId(),
                    option.getOptionValue(),
                    option.getLabel(),
                    option.getOrderIndex()
            );
        }
    }
}
