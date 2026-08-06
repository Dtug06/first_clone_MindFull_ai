package com.mindbridge.safety.review.dto;

import com.mindbridge.safety.review.ExpertReviewDecision;
import jakarta.validation.constraints.NotNull;

public record SubmitReviewRequest(
        @NotNull(message = "decision is required")
        ExpertReviewDecision decision,
        String note
) {
}