package com.mindbridge.analysis.result.repository;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link ChatAnalysisResult}.
 *
 * <p>The write side is owned exclusively by {@link com.mindbridge.analysis.result.service.ChatAnalysisResultService}
 * which uses {@code save(...)}. This repository exposes only read-side queries
 * that are backed by the indexes created in the V16 migration:
 * <ul>
 *   <li>Partial index {@code chat_analysis_results_message_active_created_desc}
 *       → {@link #findEffectiveByConversationMessageId(UUID)}</li>
 *   <li>Index {@code chat_analysis_results_user_created_desc}
 *       → {@link #findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc}</li>
 *   <li>Index on {@code supersedes_id}
 *       → {@link #findBySupersedesId(UUID)}</li>
 * </ul>
 */
@Repository
public interface ChatAnalysisResultRepository extends JpaRepository<ChatAnalysisResult, UUID> {

    /**
     * Returns the current ACTIVE result for the given message, or empty
     * if no active result exists. There is at most one ACTIVE row per
     * message at any time (enforced by a PostgreSQL trigger).
     *
     * <p>Backed by the partial index
     * {@code chat_analysis_results_message_active_created_desc}.
     */
    Optional<ChatAnalysisResult> findEffectiveByConversationMessageId(UUID conversationMessageId);

    /**
     * All results for a given run, most-recent first.
     * (Typically exactly one, but the entity is designed for multi-result
     * runs in future if needed.)
     */
    List<ChatAnalysisResult> findByAnalysisRunIdOrderByCreatedAtDesc(UUID analysisRunId);

    /**
     * All results for a given message, most-recent first.
     * Includes ACTIVE, SUPERSEDED, and INVALIDATED rows.
     */
    List<ChatAnalysisResult> findByConversationMessageIdOrderByCreatedAtDesc(UUID conversationMessageId);

    /**
     * All results for a given user in a time window, most-recent first.
     * Backed by {@code chat_analysis_results_user_created_desc} index.
     * Used by G4 behaviour_daily_features aggregation.
     */
    List<ChatAnalysisResult> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            UUID userId, OffsetDateTime from, OffsetDateTime to);

    /**
     * All results superseded by a given row (the inverse of the supersedes chain).
     * Useful for audit traversal.
     */
    List<ChatAnalysisResult> findBySupersedesId(UUID supersedesId);

    /**
     * Count of results by status for a given message.
     * Useful for sanity checks in tests and admin tooling.
     */
    long countByConversationMessageIdAndAnalysisStatus(UUID conversationMessageId, ResultAnalysisStatus status);
}
