-- V11: Behavioral Events
-- Scope: G2-T07 — log every important business action into one unified table
-- for downstream behavior analysis (G3+). Event is a write-only audit trail;
-- it never replaces the source business row.
--
-- Column choices follow docs/02_DATABASE_MVP.md §4.7:
--   user_id        : denormalized from source for fast history queries
--   event_type     : SCREAMING_SNAKE_CASE per DB-MVP §4.7 (CHAT_SESSION_STARTED,
--                    CHAT_MESSAGE_SENT, DAILY_CHECKIN_COMPLETED, ...)
--   source_type    : which table the event came from
--   source_id      : UUID of the source row (NO FK constraint — events must
--                    survive deletion of the original row for historical analysis;
--                    cross-table reference is purely informational)
--   occurred_at    : UTC timestamp at the moment the action happened
--   local_date     : user-local date at occurred_at (computed using user.timezone)
--   timezone       : IANA TZ used to compute local_date (e.g. "Asia/Ho_Chi_Minh")
--   properties     : JSONB metadata ONLY — must never contain raw content of the
--                    message/answer/option label. See G2-T07 plan §2.3 for the
--                    property shape per event_type.
--   schema_version : SMALLINT bumped when the JSON shape of properties changes
--                    for an event_type. Per DB-MVP §4.7 rule "Event schema phải
--                    có version".
--
-- Event types MVP (12, per DB-MVP §4.7). The CHECK constraint lists all 12 so
-- the table is ready for upcoming tasks (G3+ exercise/recommendation/program)
-- without requiring another ALTER. G2-T07 only hooks 4: CHAT_SESSION_STARTED,
-- CHAT_MESSAGE_SENT, DAILY_CHECKIN_COMPLETED, DAILY_CHECKIN_SKIPPED.
--
-- Idempotency (DoD §4.3 — "retry request không tạo event trùng ngoài dự kiến"):
--   UNIQUE (source_type, source_id, event_type) prevents double-write at the DB
--   layer. The service catches DataIntegrityViolationException and treats it as
--   idempotent no-op (see BehavioralEventService.record).

CREATE TABLE behavioral_events (
    id             UUID          NOT NULL DEFAULT gen_random_uuid(),
    user_id        UUID          NOT NULL,
    event_type     VARCHAR(40)   NOT NULL,
    source_type    VARCHAR(40)   NOT NULL,
    source_id      UUID          NOT NULL,
    occurred_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    local_date     DATE          NOT NULL,
    timezone       VARCHAR(50)   NOT NULL,
    properties     JSONB         NULL,
    schema_version SMALLINT      NOT NULL DEFAULT 1,

    CONSTRAINT behavioral_events_pkey PRIMARY KEY (id),
    CONSTRAINT behavioral_events_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT behavioral_events_event_type_check
        CHECK (event_type IN (
            'CHAT_SESSION_STARTED',
            'CHAT_MESSAGE_SENT',
            'DAILY_CHECKIN_COMPLETED',
            'DAILY_CHECKIN_SKIPPED',
            'EXERCISE_ASSIGNED',
            'EXERCISE_STARTED',
            'EXERCISE_COMPLETED',
            'EXERCISE_SKIPPED',
            'PROGRAM_ACCEPTED',
            'PROGRAM_PAUSED',
            'RECOMMENDATION_OPENED',
            'RECOMMENDATION_HELPFUL'
        )),
    CONSTRAINT behavioral_events_source_type_check
        CHECK (source_type IN (
            'CHAT_SESSION',
            'CONVERSATION_MESSAGE',
            'DAILY_QUESTION_ASSIGNMENT',
            'DAILY_QUESTION_ANSWER'
        )),
    CONSTRAINT behavioral_events_source_unique
        UNIQUE (source_type, source_id, event_type),
    CONSTRAINT behavioral_events_schema_version_positive_check
        CHECK (schema_version >= 1)
);

-- Index: user history, newest first (matches task §2.5: "Index theo user_id, occurred_at")
CREATE INDEX idx_behavioral_user_occurred_at
    ON behavioral_events (user_id, occurred_at DESC);

-- Index: filter by event type for cohort / analytics queries
CREATE INDEX idx_behavioral_user_event_type_occurred_at
    ON behavioral_events (user_id, event_type, occurred_at DESC);

-- Index: aggregate analytics by event type (no user filter)
CREATE INDEX idx_behavioral_event_type_occurred_at
    ON behavioral_events (event_type, occurred_at DESC);