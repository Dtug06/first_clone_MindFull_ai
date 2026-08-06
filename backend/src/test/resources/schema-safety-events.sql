-- H2-compatible DDL for the safety_events / safety_event_sources /
-- safety_actions tables. Mirrors V17 migration. Used by integration
-- tests when Flyway is disabled (test profile).
--
-- Differences from PostgreSQL:
--   * TIMESTAMPTZ maps to TIMESTAMP WITH TIME ZONE in H2 (H2 is
--     permissive and accepts TIMESTAMP without zone for the column
--     type; the entity uses OffsetDateTime which H2 round-trips).
--   * Partial indexes (WHERE status IN ('OPEN', 'UNDER_REVIEW')) are
--     dropped here   H2 syntax differs and the tests do not need
--     partial index coverage (a plain composite index suffices at
--     test scale).
--   * Foreign keys to users / risk_state_history are dropped here
--     (mirroring schema-risk-state-history.sql's approach). The
--     H2-test sandbox uses no users table; the application-layer
--     ownership check in SafetyEventService.recordLevel3Or4Event
--     enforces the user linkage. The PostgreSQL production schema
--     (V17) keeps the FKs.

CREATE TABLE IF NOT EXISTS safety_events (
    id              UUID         NOT NULL PRIMARY KEY,
    user_id         UUID         NOT NULL,
    risk_state_id   UUID         NOT NULL,
    risk_level      SMALLINT     NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    summary         CLOB         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at     TIMESTAMP    NULL,

    CONSTRAINT safety_events_status_chk
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT safety_events_risk_level_chk
        CHECK (risk_level BETWEEN 1 AND 4)
);

CREATE INDEX IF NOT EXISTS safety_events_user_active_blocking
    ON safety_events (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS safety_events_user_status_created_desc
    ON safety_events (user_id, status, created_at DESC);


CREATE TABLE IF NOT EXISTS safety_event_sources (
    id              UUID         NOT NULL PRIMARY KEY,
    safety_event_id UUID         NOT NULL,
    source_type     VARCHAR(30)  NOT NULL,
    source_id       UUID         NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT safety_event_sources_type_chk
        CHECK (source_type IN ('CHAT_ANALYSIS', 'DAILY_ANSWER',
                               'EXERCISE_SUBMISSION', 'PROGRAM_ASSESSMENT'))
);

CREATE INDEX IF NOT EXISTS safety_event_sources_event_idx
    ON safety_event_sources (safety_event_id);

CREATE INDEX IF NOT EXISTS safety_event_sources_lookup
    ON safety_event_sources (source_type, source_id);


CREATE TABLE IF NOT EXISTS safety_actions (
    id              UUID         NOT NULL PRIMARY KEY,
    safety_event_id UUID         NOT NULL,
    action_type     VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message   CLOB         NULL,
    executed_at     TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT safety_actions_type_chk
        CHECK (action_type IN ('SHOW_TEMPLATE', 'BLOCK_MATCHING',
                               'FLAG_REVIEW', 'PAUSE_PROGRAM')),
    CONSTRAINT safety_actions_status_chk
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX IF NOT EXISTS safety_actions_event_idx
    ON safety_actions (safety_event_id);

CREATE INDEX IF NOT EXISTS safety_actions_type_status_idx
    ON safety_actions (action_type, status);
-- %% G3-T12: audit columns added by V19 %%
-- Both new columns are nullable so existing PENDING action rows remain
-- valid. No FK is added in the H2 mirror (mirroring how safety_events /
-- safety_event_sources skip user-FKs in this file). The JPA entity
-- writes template_id as a plain UUID; integrity is enforced by the
-- service-layer usage, not by the test schema.
-- H2 does not accept multiple ADD COLUMN clauses in one statement, so we
-- split them. The H2 in-memory DB persists across tests in the same
-- JVM (DB_CLOSE_DELAY=-1); IF NOT EXISTS guards against duplicate-column
-- errors on the second-and-later @Sql run within a test class.
ALTER TABLE safety_actions ADD COLUMN IF NOT EXISTS template_id VARCHAR(36) NULL;
ALTER TABLE safety_actions ADD COLUMN IF NOT EXISTS template_version VARCHAR(50) NULL;

CREATE INDEX IF NOT EXISTS safety_actions_template_idx
    ON safety_actions (template_id);