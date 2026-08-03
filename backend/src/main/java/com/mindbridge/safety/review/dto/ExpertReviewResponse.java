package com.mindbridge.safety.review.dto;

import com.mindbridge.safety.review.ExpertReviewDecision;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpertReviewResponse(
        UUID id,
        UUID safetyEventId,
        UUID reviewerId,
        String reviewerDisplayName,
        ExpertReviewDecision decision,
        String note,
        OffsetDateTime createdAt
) {
}