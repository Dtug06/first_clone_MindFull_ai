package com.mindbridge.dailyquestion.dto;

import com.mindbridge.dailyquestion.domain.AssignmentStatus;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.QuestionType;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for a daily question assignment (user view).
 *
 * Maps to {@code DailyQuestionAssignmentResponse} in the OpenAPI contract.
 * The assignment is pinned to the exact template version that was active at
 * giao time — historical consistency is preserved even if a newer template
 * version is published later the same day.
 */
public record AssignmentResponse(
        UUID assignmentId,
        String templateCode,
        QuestionType questionType,
        String prompt,
        LocalDate assignedForDate,
        List<TemplateResponse.OptionResponse> options,
        boolean answered
) {
    public static AssignmentResponse from(DailyQuestionAssignment assignment) {
        var template = assignment.getTemplateVersion();
        List<TemplateResponse.OptionResponse> optionResponses = template.getOptions().stream()
                .sorted(Comparator.comparingInt(opt -> opt.getOrderIndex()))
                .map(TemplateResponse.OptionResponse::from)
                .toList();

        return new AssignmentResponse(
                assignment.getId(),
                assignment.getTemplateCode(),
                template.getQuestionType(),
                template.getPrompt(),
                assignment.getAssignedForDate(),
                optionResponses,
                assignment.getStatus() == AssignmentStatus.ANSWERED
        );
    }
}
