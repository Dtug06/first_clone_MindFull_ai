package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Request DTO for updating (creating a new version of) a daily question template.
 */
public record UpdateTemplateRequest(
        @NotNull
        QuestionType questionType,

        @NotBlank
        String prompt,

        @NotNull
        TemplateStatus status,

        List<CreateTemplateRequest.OptionRequest> options
) {}
