-- V17 - G3-T11: Safety events, sources, and actions.
-- Safety events are append-friendly records for Level 3 and Level 4 risk
-- resolutions. Each event references the triggering risk-state history row.
-- Event sources are polymorphic and are validated by the application service.
-- Child sources and actions are removed only when their owning event is removed.

CREATE TABLE safety_events (
    id              UUID         PRIMARY KEY,
    user_id         UUID         NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    risk_state_id   UUID         NOT NULL REFERENCES risk_state_history(id) ON DELETE RESTRICT,
    risk_level      SMALLINT     NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    summary         TEXT         NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    resolved_at     TIMESTAMPTZ  NULL,

    CONSTRAINT safety_events_status_chk
        CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT safety_events_risk_level_chk
        CHECK (risk_level BETWEEN 1 AND 4)
);

CREATE INDEX safety_events_user_active_blocking
    ON safety_events (user_id, created_at DESC)
    WHERE status IN ('OPEN', 'UNDER_REVIEW');

CREATE INDEX safety_events_user_status_created_desc
    ON safety_events (user_id, status, created_at DESC);

CREATE TABLE safety_event_sources (
    id              UUID         PRIMARY KEY,
    safety_event_id UUID         NOT NULL REFERENCES safety_events(id) ON DELETE CASCADE,
    source_type     VARCHAR(30)  NOT NULL,
    source_id       UUID         NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT safety_event_sources_type_chk
        CHECK (source_type IN ('CHAT_ANALYSIS', 'DAILY_ANSWER',
                               'EXERCISE_SUBMISSION', 'PROGRAM_ASSESSMENT'))
);

CREATE INDEX safety_event_sources_event_idx
    ON safety_event_sources (safety_event_id);

CREATE INDEX safety_event_sources_lookup
    ON safety_event_sources (source_type, source_id);

CREATE TABLE safety_actions (
    id              UUID         PRIMARY KEY,
    safety_event_id UUID         NOT NULL REFERENCES safety_events(id) ON DELETE CASCADE,
    action_type     VARCHAR(30)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message   TEXT         NULL,
    executed_at     TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT safety_actions_type_chk
        CHECK (action_type IN ('SHOW_TEMPLATE', 'BLOCK_MATCHING',
                               'FLAG_REVIEW', 'PAUSE_PROGRAM')),
    CONSTRAINT safety_actions_status_chk
        CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'SKIPPED'))
);

CREATE INDEX safety_actions_event_idx
    ON safety_actions (safety_event_id);

CREATE INDEX safety_actions_type_status_idx
    ON safety_actions (action_type, status);
