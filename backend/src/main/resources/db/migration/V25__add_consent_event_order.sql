-- Deterministic append order for consent events that share the same timestamp.
-- occurred_at remains the primary business timestamp; event_order is only the
-- stable tie-breaker required by the latest-event-wins invariant.

ALTER TABLE consent_events
    ADD COLUMN event_order BIGSERIAL NOT NULL;

CREATE UNIQUE INDEX consent_events_event_order_uq
    ON consent_events (event_order);

CREATE INDEX consent_events_user_type_latest_idx
    ON consent_events (user_id, consent_type, occurred_at DESC, event_order DESC);
