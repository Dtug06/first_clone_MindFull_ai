package com.mindbridge.analysis.run.dto;

import com.mindbridge.analysis.run.domain.AiAnalysisRun;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable read-only view of an {@link AiAnalysisRun} snapshot.
 *
 * <p>Returned by {@code AiAnalysisRunService.startRun(...)} to
 * callers so the caller never sees the entity class (which has
 * package-private mutators). 10-backend.mdc §32 "Do not expose JPA
 * entities directly through REST APIs" — this DTO is the contract
 * surface for ALL callers, including future REST controllers.
 *
 * <p>Defensive copies of all fields: strings are immutable, primitive
 * wrappers are immutable, OffsetDateTime is immutable.
 */
public record AiRunSummary(
        UUID id,
        UUID messageId,
        UUID userId,
        String provider,
        String model,
        String promptVersion,
        String schemaVersion,
        AiAnalysisRunStatus status,
        String inputHash,
        String outputHash,
        String errorCode,
        String errorSummary,
        int latencyMs,
        Long inputTokens,
        Long outputTokens,
        Short modelRiskLevel,
        java.math.BigDecimal confidence,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {

    /**
     * Map an entity to a snapshot. Caller must not retain the entity
     * after this projection.
     */
    public static AiRunSummary from(AiAnalysisRun row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        return new AiRunSummary(
                row.getId(),
                row.getMessageId(),
                row.getUserId(),
                row.getProvider(),
                row.getModel(),
                row.getPromptVersion(),
                row.getSchemaVersion(),
                row.getStatus(),
                row.getInputHash(),
                row.getOutputHash(),
                row.getErrorCode(),
                row.getErrorSummary(),
                row.getLatencyMs(),
                row.getInputTokens(),
                row.getOutputTokens(),
                row.getModelRiskLevel(),
                row.getConfidence(),
                row.getCreatedAt(),
                row.getStartedAt(),
                row.getCompletedAt()
        );
    }
}