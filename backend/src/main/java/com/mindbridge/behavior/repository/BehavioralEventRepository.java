package com.mindbridge.behavior.repository;

import com.mindbridge.behavior.domain.BehavioralEvent;
import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}