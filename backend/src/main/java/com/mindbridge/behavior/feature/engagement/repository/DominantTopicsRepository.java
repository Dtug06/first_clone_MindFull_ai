package com.mindbridge.behavior.feature.engagement.repository;

import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * G4-T08 read-only repository for dominant-topic aggregation.
 *
 * <p>This is intentionally separate from
 * {@link com.mindbridge.analysis.result.repository.ChatAnalysisResultRepository}
 * for two reasons:
 * <ul>
 *   <li>The downstream aggregator ({@code EngagementAndTopicsServiceImpl})
 *       is part of the {@code behavior.feature.engagement} package and
 *       keeps its dependencies cohesive. The base repository stays under
 *       {@code analysis.result}.</li>
 *   <li>This interface exposes ONLY the GROUP BY topic + ACTIVE-filter
 *       query. The base repository continues to expose row-level reads
 *       (per-message effective result, per-run history, per-message audit
 *       traversal) that other modules depend on.</li>
 * </ul>
 *
 * <p><b>Index usage.</b> The query's {@code WHERE user_id = ? AND
 * analysis_status = 'ACTIVE' AND created_at BETWEEN ? AND ?} portion is
 * served by the existing
 * {@code chat_analysis_results_user_created_desc} index
 * (V16 L40-41). The {@code analysis_status} predicate acts as a
 * residual filter on top of that index. When the data volume justifies
 * it, a future partial index {@code ON chat_analysis_results (user_id,
 * topic) WHERE analysis_status = 'ACTIVE'} would let the GROUP BY skip
 * the heap fetch entirely; that is a follow-up optimisation, not part of
 * this task (rule 30: "Khong them index khong can thiet").
 */
@Repository
public interface DominantTopicsRepository extends JpaRepository<ChatAnalysisResult, UUID> {

    /**
     * GROUP BY topic projection: one row per distinct topic, carrying the
     * count of ACTIVE {@code chat_analysis_results} rows for that topic in
     * the time window.
     *
     * <p>The optional {@code minConfidence} parameter lets the caller
     * apply the confidence floor from
     * {@link com.mindbridge.behavior.feature.engagement.config.EngagementConfig#getMinTopicConfidence()}.
     * Pass {@code null} for "no floor" (MVP default, matches G4-T04's
     * {@code FeatureConfig.defaults()}). Passing {@code null} is
     * equivalent to passing {@code 0.0}; the JPQL {@code IS NULL OR}
     * guard makes the intent explicit and survives future refactors.
     *
     * <p>The query is rerun-aware: rows with {@code analysis_status}
     * other than {@code ACTIVE} (SUPERSEDED, INVALIDATED) are filtered
     * out at the SQL level, so DoD #2 ("Topic rerun không bị đếm trùng")
     * holds without any application-layer filtering.
     *
     * @param userId        owning user
     * @param fromUtc       inclusive lower bound of the window (UTC)
     * @param toUtc         exclusive upper bound of the window (UTC)
     * @param minConfidence floor; {@code null} = no floor
     * @return one row per topic, each with the count of qualifying ACTIVE
     *         analysis rows. The implementation returns topics in any order;
     *         the caller is responsible for ordering by frequency DESC and
     *         applying the top-N cap.
     */
    @Query("""
            SELECT r.topic AS topic, COUNT(r) AS frequency
            FROM ChatAnalysisResult r
            WHERE r.userId = :userId
              AND r.analysisStatus = com.mindbridge.analysis.result.domain.ResultAnalysisStatus.ACTIVE
              AND r.createdAt >= :fromUtc
              AND r.createdAt <  :toUtc
              AND (:minConfidence IS NULL OR r.confidence >= :minConfidence)
            GROUP BY r.topic
            """)
    List<TopicCountRow> groupActiveTopicsByUserInWindow(
            @Param("userId") UUID userId,
            @Param("fromUtc") OffsetDateTime fromUtc,
            @Param("toUtc") OffsetDateTime toUtc,
            @Param("minConfidence") java.math.BigDecimal minConfidence);

    /**
     * Projection row for {@link #groupActiveTopicsByUserInWindow}.
     * Field order intentionally matches the SELECT projection so Spring
     * Data interface projection maps positionally.
     */
    interface TopicCountRow {
        String getTopic();
        long getFrequency();
    }
}
