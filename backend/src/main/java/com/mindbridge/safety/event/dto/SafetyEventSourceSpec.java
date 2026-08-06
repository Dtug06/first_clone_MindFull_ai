package com.mindbridge.safety.event.dto;

import com.mindbridge.safety.event.SafetyEventSourceType;
import java.util.UUID;

/**
 * Caller-supplied description of one source row that contributed to a
 * {@code SafetyEvent}. Used as input to
 * {@code SafetyEventService.recordLevel3Or4Event(...)}.
 *
 * <p>The polymorphic {@code (source_type, source_id)} pair lets the
 * same source row (e.g. a {@code conversation_messages.id}) participate
 * in multiple Safety Events over time without forcing DB-level FK
 * constraints across heterogeneous tables (per G3-T11 Phase 1
 * decision C5). {@code SafetyEventService} verifies ownership of the
 * referenced row at persistence time.
 *
 * @param sourceType the type of the source row. Never null.
 * @param sourceId   the id of the source row. May be {@code null} only
 *                   when the caller has no originating row id (rare;
 *                   e.g. an anonymous audit-only signal). When non-null,
 *                   the service verifies the row exists and belongs to
 *                   the event's user.
 */
public record SafetyEventSourceSpec(
        SafetyEventSourceType sourceType,
        UUID sourceId
) {
    public SafetyEventSourceSpec {
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType must not be null");
        }
    }
}