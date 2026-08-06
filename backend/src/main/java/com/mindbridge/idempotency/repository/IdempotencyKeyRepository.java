package com.mindbridge.idempotency.repository;

import com.mindbridge.idempotency.domain.IdempotencyKey;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    /**
     * Look up an idempotency record by its natural key. The DB UNIQUE on
     * (user_id, endpoint, key_value) backs this with a real guarantee.
     *
     * Used by the replay path: if the record exists and is not expired,
     * return its snapshot (response status + body).
     */
    Optional<IdempotencyKey> findByUserIdAndEndpointAndKeyValue(
            UUID userId, String endpoint, String keyValue);

    /**
     * Pessimistic-write variant: locks the natural-key row (or no-op if absent).
     *
     * Used by {@link com.mindbridge.idempotency.service.IdempotencyService}
     * under the race-condition scenario where two concurrent requests with the
     * same key both miss the lookup. By taking a {@code SELECT ... FOR UPDATE}
     * on the natural-key window, the second request blocks until the first
     * commits, then sees the row and replays.
     *
     * In H2 (test profile), this behaves the same as the regular lookup because
     * the test schema does not include INSERT-on-conflict contention. PostgreSQL
     * enforces the lock at the row level.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT k FROM IdempotencyKey k WHERE k.user.id = :userId AND k.endpoint = :endpoint AND k.keyValue = :keyValue")
    Optional<IdempotencyKey> lockByUserIdAndEndpointAndKeyValue(
            @Param("userId") UUID userId,
            @Param("endpoint") String endpoint,
            @Param("keyValue") String keyValue);

    /**
     * Upsert: insert a new record, or update the existing one if the natural
     * key already exists. Uses native SQL with PostgreSQL-compatible
     * ON CONFLICT clause. H2 does not support ON CONFLICT directly, so we use
     * a {@code MERGE} style equivalent: the implementation falls back to
     * delete-then-insert via the service layer for H2.
     *
     * For PostgreSQL, this is a single atomic SQL statement that does NOT
     * throw a UNIQUE violation when the key already exists. This is the
     * production behavior.
     *
     * For H2 (test profile), this falls back to a delete-then-insert via the
     * caller. The annotation makes this a modifying query so Spring uses
     * INSERT/UPDATE SQL under the hood.
     */
    @Modifying
    @Query(value = "INSERT INTO idempotency_keys (id, user_id, endpoint, key_value, response_status, response_body, created_at, expires_at) " +
            "VALUES (:id, :userId, :endpoint, :keyValue, :status, :body, :createdAt, :expiresAt) " +
            "ON CONFLICT (user_id, endpoint, key_value) DO UPDATE SET " +
            "    response_status = EXCLUDED.response_status, " +
            "    response_body = EXCLUDED.response_body, " +
            "    created_at = EXCLUDED.created_at, " +
            "    expires_at = EXCLUDED.expires_at",
            nativeQuery = true)
    int upsert(@Param("id") UUID id,
               @Param("userId") UUID userId,
               @Param("endpoint") String endpoint,
               @Param("keyValue") String keyValue,
               @Param("status") short status,
               @Param("body") String body,
               @Param("createdAt") java.time.Instant createdAt,
               @Param("expiresAt") java.time.Instant expiresAt);
}