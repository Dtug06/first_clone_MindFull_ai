package com.mindbridge.behavior.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.domain.BehavioralEvent;
import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.common.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Writes behavioral events for downstream analysis (G2-T07).
 *
 * This service is intentionally lightweight: one public method
 * {@link #record} that any business service can call after persisting its own
 * domain row.
 *
 * Design decisions (per G2-T07 plan):
 *
 * <h3>Transaction strategy</h3>
 * {@code record} is NOT {@code @Transactional} on its own. The intended caller
 * is already inside its own {@code @Transactional} method (e.g. {@code
 * ChatSessionService.createSession}). The event INSERT runs in the SAME
 * transaction as the parent's domain action. This gives strong consistency:
 * either both the source row and its event land, or neither does.
 *
 * <h3>Defensive guard (DoD §4.3 trade-off)</h3>
 * If the event INSERT fails for ANY reason (DB constraint violation, network
 * blip, etc.), {@code record} logs at WARN level and returns {@code null}
 * rather than propagating. This is a deliberate trade-off:
 * <ul>
 *   <li>Behavioral analysis must never block the user's primary action
 *       (creating a session, sending a message, submitting an answer).</li>
 *   <li>Loss of a single event is acceptable; rollback of the user's action is
 *       not.</li>
 *   <li>The DB UNIQUE on (source_type, source_id, event_type) makes the
 *       common duplicate-write case no-op rather than an exception.</li>
 * </ul>
 *
 * <h3>Idempotency (DoD §4.3)</h3>
 * Two layers:
 * <ol>
 *   <li>DB UNIQUE constraint rejects a literal duplicate INSERT.</li>
 *   <li>{@link BehavioralEventRepository#findBySourceTypeAndSourceIdAndEventType}
 *       provides a fast path: if a row already exists, return it without
 *       attempting another INSERT.</li>
 * </ol>
 *
 * <h3>properties safety (DoD §4.3 — no raw content)</h3>
 * Properties are serialized via Jackson {@link ObjectMapper} from a
 * {@code Map<String, Object>} provided by the caller. The service does NOT
 * inspect the map. Callers must follow the per-event-type shape documented in
 * G2-T07 plan §2.3. Raw message content, raw answer content, and option
 * labels are explicitly forbidden.
 */
@Service
public class BehavioralEventService {

    private static final Logger log = LoggerFactory.getLogger(BehavioralEventService.class);

    /** Initial schema version for all events. Bumped when JSON shape changes. */
    private static final short CURRENT_SCHEMA_VERSION = 1;

    private final BehavioralEventRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public BehavioralEventService(
            BehavioralEventRepository repository,
            UserRepository userRepository,
            ObjectMapper objectMapper,
            Clock clock) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Records a behavioral event. Intended to be called from inside an
     * existing {@code @Transactional} method right after the source row has
     * been persisted. Safe to call from any thread.
     *
     * @param userId      the user who triggered the action
     * @param eventType   the event type (must be one of {@link BehavioralEventType})
     * @param sourceType  which business table the event references
     * @param sourceId    UUID of the source row
     * @param properties  metadata map; serialized to JSON. Must not contain raw
     *                    content of the underlying message/answer/etc. Nullable.
     * @return the persisted {@link BehavioralEvent}, or {@code null} if the
     *         record failed (logged at WARN). Never throws.
     */
    public BehavioralEvent record(UUID userId, BehavioralEventType eventType,
                                   SourceType sourceType, UUID sourceId,
                                   Map<String, Object> properties) {
        if (userId == null || eventType == null || sourceType == null || sourceId == null) {
            log.warn("BehavioralEventService.record called with null arg: userId={}, eventType={}, sourceType={}, sourceId={}",
                    userId, eventType, sourceType, sourceId);
            return null;
        }

        Optional<BehavioralEvent> existing =
                repository.findBySourceTypeAndSourceIdAndEventType(sourceType, sourceId, eventType);
        if (existing.isPresent()) {
            // Idempotent path — duplicate write, no-op.
            return existing.get();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("BehavioralEventService.record: user {} not found — skipping event {}",
                    userId, eventType);
            return null;
        }

        Instant now = clock.instant();
        String tz = user.getTimezone();
        LocalDate localDate = localDateAt(now, tz);

        String propertiesJson;
        try {
            propertiesJson = (properties == null || properties.isEmpty())
                    ? null
                    : objectMapper.writeValueAsString(properties);
        } catch (JsonProcessingException e) {
            log.warn("BehavioralEventService.record: failed to serialize properties for {} {} — skipping",
                    eventType, sourceId, e);
            return null;
        }

        BehavioralEvent event = BehavioralEvent.create(
                user, eventType, sourceType, sourceId,
                now, localDate, tz, propertiesJson, CURRENT_SCHEMA_VERSION);

        try {
            return repository.save(event);
        } catch (DataIntegrityViolationException e) {
            // Race: another thread inserted the same natural key between our
            // findBy... and save(). Treat as idempotent — re-read and return.
            log.debug("BehavioralEventService.record: duplicate caught for {} {} {} — treating as idempotent",
                    eventType, sourceType, sourceId);
            return repository.findBySourceTypeAndSourceIdAndEventType(sourceType, sourceId, eventType)
                    .orElse(null);
        } catch (RuntimeException e) {
            // Defensive: any other failure must NOT propagate to the parent
            // business action. Log and return null.
            log.warn("BehavioralEventService.record: failed to persist {} for {} {} — action proceeds without event",
                    eventType, sourceType, sourceId, e);
            return null;
        }
    }

    /**
     * Resolves the user-local date for an Instant at the given IANA timezone.
     * Falls back to UTC for malformed timezone (defensive).
     */
    private LocalDate localDateAt(Instant instant, String timezone) {
        try {
            return instant.atZone(ZoneId.of(timezone)).toLocalDate();
        } catch (RuntimeException e) {
            log.warn("BehavioralEventService: invalid timezone '{}' — falling back to UTC", timezone);
            return instant.atZone(ZoneId.of("UTC")).toLocalDate();
        }
    }

    /**
     * Convenience used by tests / internal callers: look up a user from a
     * partially constructed domain object. Throws if not found. Public so
     * service tests can verify the lookup path; production callers should
     * already have a managed {@link User} reference.
     */
    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }
}