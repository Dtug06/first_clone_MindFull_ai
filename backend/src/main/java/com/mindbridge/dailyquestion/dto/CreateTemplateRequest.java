package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for creating a new daily question template.
 */
public record CreateTemplateRequest(
        @NotBlank
        @Size(max = 50)
        String code,

        @NotNull
        QuestionType questionType,

        @NotBlank
        String prompt,

        List<OptionRequest> options
) {
    /**
     * Nested option for single-choice templates.
     */
    public record OptionRequest(
            @NotBlank
            @Size(max = 50)
            String optionValue,

            @NotBlank
            String label,

            int orderIndex
    ) {}
}
