-- H2-compatible DDL for behavioral_events table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V11__create_behavioral_events.sql.

CREATE TABLE IF NOT EXISTS behavioral_events (
    id             VARCHAR(36)   NOT NULL PRIMARY KEY,
    user_id        VARCHAR(36)   NOT NULL,
    event_type     VARCHAR(40)   NOT NULL,
    source_type    VARCHAR(40)   NOT NULL,
    source_id      VARCHAR(36)   NOT NULL,
    occurred_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    local_date     DATE          NOT NULL,
    timezone       VARCHAR(50)   NOT NULL,
    properties     VARCHAR(4000) NULL,
    schema_version SMALLINT      NOT NULL DEFAULT 1,
    CONSTRAINT behavioral_events_source_unique UNIQUE (source_type, source_id, event_type)
);

CREATE INDEX IF NOT EXISTS idx_behavioral_user_occurred_at
    ON behavioral_events (user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_behavioral_user_event_type_occurred_at
    ON behavioral_events (user_id, event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_behavioral_event_type_occurred_at
    ON behavioral_events (event_type, occurred_at DESC);