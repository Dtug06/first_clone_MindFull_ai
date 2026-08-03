package com.mindbridge.analysis.result.dto;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Immutable read-only view of a {@link ChatAnalysisResult} snapshot.
 *
 * <p>Returned by {@link com.mindbridge.analysis.result.service.ChatAnalysisResultService}
 * so callers never see the JPA entity class (which has package-private mutators
 * for the SUPERSEDED/INVALIDATED transitions). This DTO is the contract surface
 * for all callers, including future REST controllers.
 *
 * <p>Defensive copies: strings, primitives, and OffsetDateTime are immutable.
 * The {@code signals} and {@code evidenceSpans} fields are already immutable
 * string arrays (serialised JSON) from the entity.
 */
public record ChatAnalysisResultSummary(
        UUID id,
        UUID analysisRunId,
        UUID conversationMessageId,
        UUID userId,
        String topic,
        String emotion,
        String intent,
        List<String> signals,
        List<String> evidenceSpans,
        short modelRiskLevel,
        BigDecimal confidence,
        ResultAnalysisStatus analysisStatus,
        UUID supersedesId,
        OffsetDateTime createdAt
) {

    /**
     * Map an entity to a snapshot DTO.
     *
     * @param row the entity (must not be null).
     * @return an immutable snapshot.
     */
    public static ChatAnalysisResultSummary from(ChatAnalysisResult row) {
        if (row == null) {
            throw new IllegalArgumentException("row must not be null");
        }
        return new ChatAnalysisResultSummary(
                row.getId(),
                row.getAnalysisRunId(),
                row.getConversationMessageId(),
                row.getUserId(),
                row.getTopic(),
                row.getEmotion(),
                row.getIntent(),
                row.getSignalsAsList(),
                row.getEvidenceSpansAsList(),
                row.getModelRiskLevel(),
                row.getConfidence(),
                row.getAnalysisStatus(),
                row.getSupersedesId(),
                row.getCreatedAt()
        );
    }
}
