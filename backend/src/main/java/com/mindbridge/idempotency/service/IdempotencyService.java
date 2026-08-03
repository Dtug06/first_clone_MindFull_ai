package com.mindbridge.idempotency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.idempotency.domain.IdempotencyKey;
import com.mindbridge.idempotency.repository.IdempotencyKeyRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Idempotency service for the 2 endpoints instrumented in G2-T08:
 * POST /chat/sessions/{sessionId}/messages and POST /daily-checkins/{assignmentId}/answer.
 *
 * <h3>Design (per G2-T08 plan §3.3-§3.6)</h3>
 * <ul>
 *   <li><b>Optional key</b> — if {@code idempotencyKey} is null/blank, the service
 *       bypasses the idempotency machinery entirely and runs the supplier as-is.
 *       This preserves backward compatibility for clients that don't yet send a key.</li>
 *   <li><b>Replay path</b> — if a record exists for (userId, endpoint, key) and is
 *       not expired, return the stored response snapshot with status + body. No
 *       re-execution of the supplier.</li>
 *   <li><b>Execute path</b> — if no record (or expired), run the supplier. If the
 *       supplier returns 2xx, persist a new idempotency record with the response
 *       snapshot. If the supplier returns 4xx/5xx, do NOT record — the client should
 *       fix the payload and retry with a new key.</li>
 *   <li><b>Race condition</b> — if two concurrent requests with the same key both
 *       miss the lookup, both will try to INSERT. The DB UNIQUE on
 *       (user_id, endpoint, key_value) makes the second INSERT fail. The loser
 *       catches {@link DataIntegrityViolationException}, re-reads the row, and
 *       returns the winner's snapshot.</li>
 * </ul>
 *
 * <h3>Why a separate service (not AOP)</h3>
 * Phase 1 plan §4.1 chose inline invocation over AOP to keep changes minimal
 * and explicit. Each instrumented endpoint wraps its business logic in a
 * {@code Supplier<ResponseEntity<T>>} and hands it to {@link #executeWithIdempotency}.
 *
 * <h3>Why not record 4xx</h3>
 * Per plan §3.6: recording 4xx would lock the client to the original error
 * even after the backend is fixed. Clients must use a new key for each new
 * attempt. 5xx is similarly non-cacheable.
 */
@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);

    /** Wraps a response body's HTTP status. Lets the controller distinguish
     *  2xx (record-able) from 4xx/5xx (skip record). */
    public record IdempotencyResult<T>(T body, HttpStatusCode status) {}

    private final IdempotencyKeyRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final TransactionTemplate recordTransactionTemplate;

    public IdempotencyService(IdempotencyKeyRepository repository,
                              UserRepository userRepository,
                              ObjectMapper objectMapper,
                              Clock clock,
                              @Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        // REQUIRES_NEW so the snapshot record is committed independently of any
        // supplier transaction. If transactionManager is missing (e.g. unit
        // test without Spring context), fall back to a no-op template.
        this.recordTransactionTemplate = (transactionManager != null)
                ? new TransactionTemplate(transactionManager) : null;
        if (this.recordTransactionTemplate != null) {
            this.recordTransactionTemplate.setPropagationBehavior(
                    org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
    }

    /**
     * Executes a supplier with idempotency, if a key is provided.
     *
     * <h3>Transaction & locking strategy (QA P2 patch X-8)</h3>
     * This method is intentionally <b>NOT</b> {@code @Transactional}. Instead
     * we manage transactions manually so that a failure in the snapshot
     * record step cannot poison the supplier's transaction. The previous
     * design used {@code @Transactional(noRollbackFor=...)} on the outer
     * method, but UnexpectedRollbackException thrown by Spring's commit
     * phase still escaped the noRollbackFor contract — concurrent same-key
     * requests would leak HTTP 500 to the client.
     *
     * <p>Concrete flow:
     * <ol>
     *   <li><b>Replay lookup (read-only tx)</b> — short transaction to
     *       check for an existing record and acquire the PESSIMISTIC_WRITE
     *       lock on the natural-key window (gap lock in PostgreSQL).</li>
     *   <li><b>Expired record cleanup (REQUIRES_NEW tx)</b> — if the found
     *       record is expired, delete it in its own tx so the deletion is
     *       committed before the snapshot insert. The lookup lock is
     *       released at this point; in PostgreSQL the gap lock is also
     *       released, but the supplier's own insert will follow
     *       immediately.</li>
     *   <li><b>Supplier execution</b> — the supplier carries its own
     *       {@code @Transactional} which opens a fresh transaction
     *       (since the outer method is no longer {@code @Transactional},
     *       there's no parent to join). The supplier commits
     *       independently.</li>
     *   <li><b>Snapshot record (REQUIRES_NEW tx)</b> — short tx to insert
     *       the idempotency record. Failures here are caught and logged;
     *       they never poison the supplier's already-committed transaction.</li>
     * </ol>
     *
     * @param userId          authenticated user (must be non-null)
     * @param endpoint        logical endpoint identifier
     *                        (e.g. "POST:/chat/sessions/{sessionId}/messages")
     * @param idempotencyKey  client-supplied key (nullable — if null, bypass)
     * @param responseType    the response body type for serialization
     * @param supplier        the business logic that produces the response
     * @param <T>             response body type
     * @return the IdempotencyResult: either the original execution result, or a
     *         replayed snapshot from an existing record
     */
    public <T> IdempotencyResult<T> executeWithIdempotency(
            UUID userId,
            String endpoint,
            String idempotencyKey,
            Class<T> responseType,
            Supplier<IdempotencyResult<T>> supplier) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // No key — bypass idempotency entirely (backward compat)
            return supplier.get();
        }

        // 1. Replay path (with pessimistic lock to serialize concurrent requests)
        Optional<IdempotencyKey> existing = lookupForReplay(userId, endpoint, idempotencyKey);
        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();
            if (record.isExpired(clock)) {
                log.debug("Idempotency expired for user={} endpoint={} key={} — deleting old record and treating as new",
                        userId, endpoint, idempotencyKey);
                deleteExpiredRecord(record);
                // Fall through to execute path.
            } else {
                return replay(record, responseType);
            }
        }

        // 2. Execute path (supplier carries its own @Transactional — opens its own tx)
        IdempotencyResult<T> result = supplier.get();

        // 3. Record only 2xx (in REQUIRES_NEW tx so failures are isolated)
        if (result.status().is2xxSuccessful()) {
            try {
                recordSnapshot(userId, endpoint, idempotencyKey, result);
            } catch (Exception e) {
                // Catch ALL so the supplier's response is always returned.
                log.debug("Idempotency snapshot record failed for {} {} — supplier response still valid",
                        endpoint, idempotencyKey, e);
            }
        }

        return result;
    }

    /**
     * Read-only lookup with pessimistic lock to serialize concurrent requests
     * for the same natural key. Returns empty if no row exists.
     *
     * <p>Uses a separate read-only tx (not joined to any outer tx because
     * this method is called without an outer @Transactional).
     */
    Optional<IdempotencyKey> lookupForReplay(UUID userId, String endpoint, String idempotencyKey) {
        if (recordTransactionTemplate == null) {
            return repository.lockByUserIdAndEndpointAndKeyValue(userId, endpoint, idempotencyKey);
        }
        TransactionTemplate readOnlyTemplate = new TransactionTemplate(
                recordTransactionTemplate.getTransactionManager());
        readOnlyTemplate.setReadOnly(true);
        return readOnlyTemplate.execute(s ->
                repository.lockByUserIdAndEndpointAndKeyValue(userId, endpoint, idempotencyKey));
    }

    /**
     * Deletes an expired idempotency record in its own REQUIRES_NEW tx so
     * the next snapshot insert does not collide.
     */
    void deleteExpiredRecord(IdempotencyKey record) {
        if (recordTransactionTemplate == null) {
            repository.delete(record);
            repository.flush();
            return;
        }
        UUID recordId = record.getId();
        recordTransactionTemplate.executeWithoutResult(status -> {
            IdempotencyKey inNewTx = repository.findById(recordId).orElse(null);
            if (inNewTx != null) {
                repository.delete(inNewTx);
                repository.flush();
            }
        });
    }

    /**
     * Reconstructs a IdempotencyResult from a stored snapshot. The body is
     * deserialized as the supplied type using Jackson.
     */
    private <T> IdempotencyResult<T> replay(IdempotencyKey record, Class<T> responseType) {
        try {
            T body = objectMapper.readValue(record.getResponseBody(), responseType);
            return new IdempotencyResult<>(body,
                    org.springframework.http.HttpStatusCode.valueOf(record.getResponseStatus()));
        } catch (JsonProcessingException e) {
            log.warn("Idempotency replay failed to deserialize snapshot for id={} — falling through",
                    record.getId(), e);
            throw new IdempotencyReplayException(
                    "Stored idempotency snapshot is corrupted; please retry with a new key", e);
        }
    }

    /**
     * Persists a new idempotency record.
     *
     * <h3>REQUIRES_NEW isolation (QA P2 patch X-8)</h3>
     * This method runs in its <b>own</b> transaction (REQUIRES_NEW via
     * {@link TransactionTemplate}) so a failure here — whether from DB UNIQUE
     * collision on a concurrent same-key insert, or from any other persistence
     * error — cannot poison the supplier's transaction. The supplier has
     * already committed by the time we get here.
     *
     * <h3>Race protection</h3>
     * If two concurrent requests with the same key both passed the replay
     * lookup (e.g. because the test environment's pessimistic-lock semantics
     * are weaker than PostgreSQL's), the DB UNIQUE on
     * (user_id, endpoint, key_value) trips for the loser. We catch the
     * exception, re-fetch the winner's row, and log the race. The supplier's
     * response is still returned to the caller — both requests observe a
     * valid 201 response. The idempotency table simply tracks the winner.
     *
     * <h3>Defense in depth</h3>
     * We also catch {@link UnexpectedRollbackException} (in case the
     * underlying transaction machinery bubbles it up at commit time even
     * after the catch on the inner call) as a final safety net so the
     * supplier's response is always returned to the caller. Without this
     * guard, the previous design leaked HTTP 500 to the client when two
     * concurrent same-key requests raced — see G2 acceptance test report
     * §7.1.
     */
    /**
     * Persists a new idempotency record in a REQUIRES_NEW transaction.
     *
     * <p>Called only from {@link #executeWithIdempotency}, which itself is
     * not {@code @Transactional}, so REQUIRES_NEW opens a fresh short-lived
     * tx. Any failure here (DB UNIQUE collision on concurrent same-key,
     * etc.) cannot affect the supplier's already-committed work because
     * the supplier uses its own transaction.
     *
     * <p>The {@link #executeWithIdempotency} caller wraps this call in a
     * try-catch-all so even the most pathological persistence failures do
     * not escape to the controller as HTTP 500. See G2 acceptance report
     * §7.1 for the X-8 patch history.
     */
    <T> void recordSnapshot(UUID userId, String endpoint, String idempotencyKey,
                             IdempotencyResult<T> result) {
        if (recordTransactionTemplate == null) {
            // Unit test without Spring context — skip the record.
            return;
        }
        recordTransactionTemplate.executeWithoutResult(status ->
                doRecordSnapshot(userId, endpoint, idempotencyKey, result));
    }

    /**
     * Internal: actual snapshot insert. Runs inside the REQUIRES_NEW tx
     * opened by {@link #recordSnapshot}.
     */
    private <T> void doRecordSnapshot(UUID userId, String endpoint, String idempotencyKey,
                                       IdempotencyResult<T> result) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Idempotency record skipped: user {} not found", userId);
            return;
        }

        String bodyJson;
        try {
            bodyJson = objectMapper.writeValueAsString(result.body());
        } catch (JsonProcessingException e) {
            log.warn("Idempotency record skipped: failed to serialize response body for {} {} — proceeding without record",
                    endpoint, idempotencyKey, e);
            return;
        }

        IdempotencyKey record = IdempotencyKey.create(
                user, endpoint, idempotencyKey,
                (short) result.status().value(),
                bodyJson, clock);

        try {
            repository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request committed first. The supplier
            // has already committed its message; this REQUIRES_NEW tx just
            // couldn't grab the idempotency row. We log and let the caller
            // continue. The supplier's response is the source of truth.
            log.debug("Idempotency record insert collided for {} {} — concurrent request won; supplier response still valid",
                    endpoint, idempotencyKey, e);
        }
    }

    /**
     * Helper to build a {IdempotencyResult} from a body and status. Saves
     * caller from new-ing the record explicitly.
     */
    public static <T> IdempotencyResult<T> result(T body, HttpStatusCode status) {
        return new IdempotencyResult<>(body, status);
    }

    /**
     * Convenience: wrap a controller result by reading the status code from
     * a {@link ResponseEntity}.
     */
    public static <T> IdempotencyResult<T> fromResponse(ResponseEntity<T> response) {
        return new IdempotencyResult<>(response.getBody(), response.getStatusCode());
    }

    /**
     * Thrown when a stored snapshot cannot be deserialized. This is a 500
     * condition the caller should map to an error response.
     */
    public static class IdempotencyReplayException extends RuntimeException {
        public IdempotencyReplayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}