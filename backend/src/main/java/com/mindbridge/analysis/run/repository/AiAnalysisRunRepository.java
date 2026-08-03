package com.mindbridge.analysis.run.repository;

import com.mindbridge.analysis.run.domain.AiAnalysisRun;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for {@link AiAnalysisRun}.
 *
 * <p>Only the read-side query methods defined in the G3-T04 plan are
 * exposed; the write side is owned exclusively by
 * {@code AiAnalysisRunService} which uses {@code save(...)} (the
 * default JpaRepository method) plus the package-private transition
 * methods on the entity.
 *
 * <p>All queries listed here are backed by the indexes created in
 * the V15 migration:
 * <ul>
 *   <li>{@code ai_analysis_runs_message_created_desc} —
 *       {@link #findByMessageIdOrderByCreatedAtDesc(UUID)}</li>
 *   <li>{@code ai_analysis_runs_status_created_desc} —
 *       {@link #findByStatusOrderByCreatedAtDesc(AiAnalysisRunStatus)}</li>
 *   <li>{@code ai_analysis_runs_created_at} —
 *       {@link #findByCreatedAtBetween(OffsetDateTime, OffsetDateTime)}</li>
 * </ul>
 *
 * <p>Ownership query (findByMessageIdAndUserId) is defined for future
 * REST consumers (T11+); T04 does not call it directly.
 */
@Repository
public interface AiAnalysisRunRepository extends JpaRepository<AiAnalysisRun, UUID> {

    /**
     * Most-recent-first list of all runs that targeted the given
     * conversation message. Backed by the
     * {@code ai_analysis_runs_message_created_desc} index.
     */
    List<AiAnalysisRun> findByMessageIdOrderByCreatedAtDesc(UUID messageId);

    /**
     * Most-recent-first list of all runs in the given status.
     * Backed by the {@code ai_analysis_runs_status_created_desc}
     * index.
     */
    List<AiAnalysisRun> findByStatusOrderByCreatedAtDesc(AiAnalysisRunStatus status);

    /**
     * Most-recent-first list of runs created in the inclusive time
     * range. Backed by the {@code ai_analysis_runs_created_at} index.
     */
    List<AiAnalysisRun> findByCreatedAtBetweenOrderByCreatedAtDesc(
            OffsetDateTime from, OffsetDateTime to);

    /**
     * Ownership query for future REST consumers. Returns runs that
     * targeted the given message AND that belong to the given user
     * (denormalized user_id column). Returns empty list when no
     * match — callers must NOT default to "all rows" on empty.
     *
     * <p>T04 does not call this directly; defined here so a future
     * controller can use it without modifying the repository.
     */
    List<AiAnalysisRun> findByMessageIdAndUserIdOrderByCreatedAtDesc(
            UUID messageId, UUID userId);

    /**
     * Count of runs in a given status. Useful for ops dashboards.
     */
    long countByStatus(AiAnalysisRunStatus status);
}