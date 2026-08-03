package com.mindbridge.safety.review.dto;

import com.mindbridge.safety.event.SafetyEventStatus;
import com.mindbridge.safety.event.domain.SafetyAction;
import com.mindbridge.safety.event.domain.SafetyEventSource;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SafetyEventDetailResponse(
        UUID id,
        UUID userId,
        short riskLevel,
        SafetyEventStatus status,
        String summary,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt,
        List<SourceResponse> sources,
        List<ActionResponse> actions,
        List<ExpertReviewResponse> reviews
) {
    public record SourceResponse(UUID id, String sourceType, UUID sourceId, OffsetDateTime createdAt) {
        public SourceResponse(SafetyEventSource source) {
            this(source.getId(), source.getSourceType().name(), source.getSourceId(), source.getCreatedAt());
        }
    }

    public record ActionResponse(
            UUID id, String actionType, String status, String errorMessage,
            OffsetDateTime executedAt, OffsetDateTime createdAt, UUID templateId, String templateVersion
    ) {
        public ActionResponse(SafetyAction action) {
            this(action.getId(), action.getActionType().name(), action.getStatus().name(),
                    action.getErrorMessage(), action.getExecutedAt(), action.getCreatedAt(),
                    action.getTemplateId(), action.getTemplateVersion());
        }
    }
}