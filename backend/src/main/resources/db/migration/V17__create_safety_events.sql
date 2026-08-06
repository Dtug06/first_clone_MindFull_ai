-- V17   G3-T11: Safety events, sources, and actions
--
-- Stores the safety intervention workflow triggered by Level 3 or
-- Level 4 risk resolutions. Three tables form a 1-to-many chain:
--
--   safety_events          1%%%%%%<  safety_event_sources   (polymorphic)
--        %
--        %%%%%%%<  safety_actions               (action catalog rows)
--
-- Schema mirrors the user-approved G3-T11 Phase 1 spec with the
-- following additions vs DB-MVP §6.2/§6.3:
--   * `safety_events.risk_state_id` is a new FK column to the
--     `risk_state_history.id` row that TRIGGERED the event. This lets
--     audit reconstruct exactly which decision produced the event
--     without re-running the resolver. RESTRICT (not CASCADE) on the
--     FK   we never want a delete on the history to silently drop an
--     event ("Do not hard-delete sensitive history without an approved
--     retention rule", rule 30). Note: `risk_state_history` itself
--     cascades on `users` deletion (V14), so the chain still
--     collapses when a user is hard-deleted.
--   * `safety_events.risk_level` is denormalised snapshot, NOT a FK  
--     events must remain auditable even if a later no-downgrade-guard
--     decision in the resolver rewrites the user's history. CHECK
--     1..4.
--   * `safety_event_sources.source_id` is a polymorphic reference.
--     Per the G3-T11 Phase 1 decision C5, the trade-off of NO DB-level
--     FK on the polymorphic column is accepted: application code
--     (`SafetyEventService`) verifies ownership/identity for every
--     source. Documented in the entity JavaDoc.
--   * `safety_actions` was previously listed as DEFERRED in
--     `docs/02_DATABASE_MVP.md` §12. G3-T11 promotes it to MVP scope
--     (the user-approved Phase 1 spec). The DB-MVP file is updated
--     accordingly. Action rows are created with status = PENDING at
--     T11; runtime execution is the responsibility of downstream
--     tasks (T12 SHOW_TEMPLATE, T13 FLAG_REVIEW, G6 BLOCK_MATCHING,
--     future PAUSE_PROGRAM). One action failing MUST NOT block the
--     others (per the G3-T11 Phase 1 decision C7)   each action has
--     its own `status` + `error_message` and is executed
--     independently by its owning module.
--
-- IMPORTANT invariants:
--   * Append-friendly: every event starts OPEN. Status transitions to
--     UNDER_REVIEW / RESOLVED / DISMISSED will be implemented in
--     G3-T13 (Expert Review). T11 ships no transition code.
--   * Every event has at least one source and at least one action  
--     enforced by application-layer validation in
--     `SafetyEventService.recordLevel3Or4Event` (no DB-level
--     "required child" constraint because PostgreSQL does not have a
--     clean way to require "1 row in another table exists" without
--     triggers; an app-layer check is clearer and tested).
--   * No seed rows. The schema is empty until the resolver records a
--     real L3/L4 decision (per docs/04 §4 "M×i Safety Event ph£i có
--     ít nh¥t mÙt source").
--   * Cascade policy: deleting an event deletes its sources and
--     actions (CASCADE)   they are children of the event. Deleting a
--     user is RESTRICT at this layer (the upstream `risk_state_history`
--     row already cascades on user deletion; the event FK to that
--     row uses RESTRICT so a delete on `users` will eventually
--     cascade through `risk_state_history`).
--   * Audit: every event write emits an `audit_logs` row via
--     `AuditService.record(category=SAFETY, action=SAFETY_EVENT_OPENED,
--     actorType=SYSTEM, subjectType=SAFETY_EVENT, subjectId=event.id)`
--     so audit can correlate the event with its trigger. Audit runs
--     in REQUIRES_NEW (existing `AuditService` semantics) so audit
--     failures never break the caller's transaction.

-- %% safety_events %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
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

-- Hot path 1: "is this user currently blocked?"   partial index keeps
-- the query cheap even if the table grows.
CREATE INDEX safety_events_user_active_blocking
    ON safety_events (user_id, created_at DESC)
    WHERE status IN ('OPEN', 'UNDER_REVIEW');

-- Hot path 2: audit / support tooling that lists events by user
-- regardless of status, newest first.
CREATE INDEX safety_events_user_status_created_desc
    ON safety_events (user_id, status, created_at DESC);


-- %% safety_event_sources %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
-- Polymorphic reference to the row that triggered this event. The
-- (source_type, source_id) pair identifies the source row; the
-- application layer (`SafetyEventService`) verifies ownership via
-- the typed repository. NO DB-level FK on `source_id` by design   the
-- trade-off is documented in `SafetyEventSource.java` JavaDoc.
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

-- Reverse lookup: "which events did this source row contribute to?"
-- Used by audit tooling.
CREATE INDEX safety_event_sources_lookup
    ON safety_event_sources (source_type, source_id);


-- %% safety_actions %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
-- One Safety Event spawns N Safety Actions. Each action is independent:
-- one FAILED action MUST NOT block another SUCCEEDED action. Execution
-- is the responsibility of the owning module; T11 only persists the
-- row with status = PENDING.
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

-- Operational view: "which SHOW_TEMPLATE actions are stuck PENDING?"
-- Useful when T12 ships and we need to monitor the executor.
CREATE INDEX safety_actions_type_status_idx
    ON safety_actions (action_type, status);