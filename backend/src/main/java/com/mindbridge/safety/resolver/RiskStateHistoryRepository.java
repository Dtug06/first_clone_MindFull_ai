package com.mindbridge.safety.resolver;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for the append-only {@link RiskStateHistory}.
 *
 * <p>Only one write method exists in practice — calling code persists
 * new rows via {@link JpaRepository#save(Object)} from
 * {@code SafetyResolverService.resolve(...)}. The repository does not
 * expose update or delete methods; the append-only contract is
 * enforced by the entity (no setters, no {@code @PreUpdate}/
 * {@code @PreRemove}) and the schema.
 *
 * <p>The hot read path is
 * {@link #findFirstByUserIdOrderByOccurredAtDescIdDesc(UUID)} — it is
 * used by the resolver itself (to read the previous state when
 * computing the next one), by the Safety Gate in the matching
 * pipeline (planned G6), and by any audit / support tooling that needs
 * the current risk state for a user. The {@code id DESC} tie-break is
 * intentional and mirrors the G2 acceptance decision #2 fix on
 * {@code ConsentEventRepository.findLatestByUserAndType} so the
 * behaviour stays deterministic even when two rows share an
 * {@code occurred_at} (rare but possible in tests).
 */
@Repository
public interface RiskStateHistoryRepository extends JpaRepository<RiskStateHistory, UUID> {

    /**
     * Returns the latest history row for a user, or empty when the
     * user has never been resolved. Latest means the row with the
     * greatest {@code occurred_at}; ties are broken by the greatest
     * {@code id} (UUID v4 random — adequate tie-break for MVP).
     *
     * <p>Backed by the {@code risk_state_history_user_occurred_desc}
     * index defined in V14.
     */
    Optional<RiskStateHistory> findFirstByUserIdOrderByOccurredAtDescIdDesc(UUID userId);

    /**
     * Full history for a user, newest first. Intended for audit /
     * support tooling and for tests that verify the append-only
     * contract (e.g. "the second resolve inserted a second row").
     *
     * <p>Not paginated in MVP — a typical user has at most a few rows
     * per day. If a user accumulates thousands of rows, add a paged
     * variant later (separate task).
     */
    List<RiskStateHistory> findByUserIdOrderByOccurredAtDescIdDesc(UUID userId);

    /** Total rows for a user. Used by tests to assert append-only. */
    long countByUserId(UUID userId);
}
