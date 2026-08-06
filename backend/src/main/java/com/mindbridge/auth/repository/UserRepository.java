package com.mindbridge.auth.repository;

import com.mindbridge.auth.domain.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the {@code users} table.
 *
 * @findByEmailIgnoreCase uses citext case-insensitivity on the DB side.
 *
 * <h3>G4-T05 status filter</h3>
 * {@link #findByStatusOrderByIdAsc(User.UserStatus, Pageable)} backs the
 * daily feature aggregation job's chunked iteration (Q2 choice). It
 * restricts to ACTIVE users so DELETED/SUSPENDED users are never
 * visited. Ordered by {@code id} for deterministic batch boundaries
 * across re-runs (id is UUID v4; ordering is still stable enough for
 * our purposes -- the job is idempotent so non-strict ordering is fine).
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Paginated lookup of users with the given status, ordered by id
     * ascending for stable chunk boundaries.
     *
     * <p>Used by G4-T05 {@code DailyFeatureAggregationServiceImpl} to
     * iterate the ACTIVE user base in chunks of {@code batch-size}
     * (Q2 decision). Pagination size is supplied by the caller.
     *
     * @param status   user status to filter on (typically {@code ACTIVE})
     * @param pageable page request -- typically {@code PageRequest.of(page, batchSize)}
     * @return list of users in the requested page (empty when past end)
     */
    List<User> findByStatusOrderByIdAsc(User.UserStatus status, Pageable pageable);

    /**
     * Total count of users with the given status. Used by G4-T05 to
     * pre-size progress tracking before iterating batches.
     */
    long countByStatus(User.UserStatus status);
}
