package com.mindbridge.behavior.repository;

import com.mindbridge.behavior.domain.BehavioralEvent;
import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.feature.dto.BehavioralEventCountsRow;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BehavioralEventRepository extends JpaRepository<BehavioralEvent, UUID> {

    /**
     * Looks up an event by its natural key. Used by the service to make
     * {@code record(...)} idempotent — when the same (sourceType, sourceId,
     * eventType) is written twice (e.g. parent service retried by accident),
     * the second call gets back the existing row instead of a duplicate.
     *
     * The DB UNIQUE on (source_type, source_id, event_type) backs this with a
     * real guarantee.
     */
    Optional<BehavioralEvent> findBySourceTypeAndSourceIdAndEventType(
            SourceType sourceType, UUID sourceId, BehavioralEventType eventType);

    /**
     * G4-T03: counts the 4 behavioral event categories that drive the
     * engagement features (chat-message, chat-session, checkin-completed,
     * checkin-skipped) for one user in a closed-open UTC time window
     * {@code [fromUtc, toUtc)}.
     *
     * <p>CHAT_SESSION_STARTED is counted DISTINCT by source_id (= chat
     * session id) so that a long session that emits many heartbeats only
     * contributes 1 to the active-session tally. The other three event
     * types are naturally counted by row (one row == one occurrence).
     *
     * <p>The query is a single SELECT with 4 conditional aggregates - O(1)
     * in time and DB load regardless of the user's volume. The window is
     * supplied in UTC because {@code occurred_at} is stored in UTC and the
     * caller already converted the user's local-date window.
     *
     * @param userId  target user
     * @param fromUtc inclusive lower bound (UTC Instant)
     * @param toUtc   exclusive upper bound (UTC Instant)
     */
    @Query("""
            SELECT
              COUNT(CASE WHEN e.eventType = com.mindbridge.behavior.domain.BehavioralEventType.CHAT_MESSAGE_SENT THEN 1 ELSE NULL END)            AS chatMessageCount,
              COUNT(DISTINCT CASE WHEN e.eventType = com.mindbridge.behavior.domain.BehavioralEventType.CHAT_SESSION_STARTED THEN e.sourceId ELSE NULL END) AS activeChatSessionCount,
              COUNT(CASE WHEN e.eventType = com.mindbridge.behavior.domain.BehavioralEventType.DAILY_CHECKIN_COMPLETED THEN 1 ELSE NULL END)     AS checkinCompletedCount,
              COUNT(CASE WHEN e.eventType = com.mindbridge.behavior.domain.BehavioralEventType.DAILY_CHECKIN_SKIPPED THEN 1 ELSE NULL END)       AS checkinSkippedCount
            FROM BehavioralEvent e
            WHERE e.user.id = :userId
              AND e.occurredAt >= :fromUtc
              AND e.occurredAt <  :toUtc
            """)
    BehavioralEventCountsRow aggregateByUserAndDay(
            @Param("userId") UUID userId,
            @Param("fromUtc") Instant fromUtc,
            @Param("toUtc") Instant toUtc);
}