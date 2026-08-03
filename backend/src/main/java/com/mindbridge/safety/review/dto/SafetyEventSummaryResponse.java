package com.mindbridge.safety.review.dto;

import com.mindbridge.safety.event.SafetyEventStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record SafetyEventSummaryResponse(
        UUID id,
        UUID userId,
        short riskLevel,
        SafetyEventStatus status,
        String summary,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt,
        int reviewCount
) {
}