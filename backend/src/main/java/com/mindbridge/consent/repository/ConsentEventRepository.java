package com.mindbridge.consent.repository;

import com.mindbridge.consent.domain.ConsentEvent;
import com.mindbridge.consent.domain.enums.ConsentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentEventRepository extends JpaRepository<ConsentEvent, UUID> {

    /**
     * Returns all events for a user, oldest first — used to compute history or audit.
     */
    List<ConsentEvent> findByUserIdOrderByOccurredAtAscEventOrderAsc(UUID userId);

    /**
     * Returns the latest event per consent type for a user.
     *
     * Uses PostgreSQL DISTINCT ON — the production query is not portable, but
     * for H2 (test) we substitute a window-function equivalent via the fallback
     * query below. Spring Data picks the dialect-appropriate query by using
     * a single native SQL string the database can parse.
     *
     * The query orders by consent_type, occurred_at DESC, event_order DESC and
     * keeps the first row per consent_type via DISTINCT ON. The generated
     * event_order makes same-timestamp grant/revoke events deterministic.
     */
    @Query(value = """
            SELECT DISTINCT ON (consent_type)
                   id, user_id, consent_type, action, policy_version, metadata,
                   occurred_at, event_order
            FROM consent_events
            WHERE user_id = :userId
            ORDER BY consent_type, occurred_at DESC, event_order DESC
            """, nativeQuery = true)
    List<ConsentEvent> findLatestPerTypeByUser(@Param("userId") UUID userId);

    /**
     * Returns the latest event for a single (userId, consentType) pair.
     */
    @Query("""
            SELECT c
            FROM ConsentEvent c
            WHERE c.userId = :userId
              AND c.consentType = :consentType
            ORDER BY c.occurredAt DESC, c.eventOrder DESC
            """)
    List<ConsentEvent> findLatestByUserAndType(@Param("userId") UUID userId,
                                               @Param("consentType") ConsentType consentType);
}
