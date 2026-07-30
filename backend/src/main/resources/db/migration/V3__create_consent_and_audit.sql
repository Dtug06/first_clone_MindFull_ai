-- V3__create_consent_and_audit.sql
-- MindBridge AI — G1-T04
-- consent_events : append-only audit of consent grants/revocations per user.
-- audit_logs     : generic audit trail for important system actions.

-- ── consent_events ─────────────────────────────────────────────────────────
CREATE TABLE consent_events (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    consent_type    VARCHAR(50) NOT NULL,
    action          VARCHAR(20) NOT NULL,
    policy_version  VARCHAR(50) NOT NULL DEFAULT '1.0',
    metadata        JSONB,
    occurred_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT consent_events_pkey            PRIMARY KEY (id),
    CONSTRAINT consent_events_user_fk         FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT consent_type_check             CHECK (consent_type IN ('CHAT_ANALYSIS', 'PERSONALIZATION', 'EXPERT_SHARING')),
    CONSTRAINT consent_action_check           CHECK (action        IN ('GRANTED', 'REVOKED'))
);

-- Primary access pattern: "what is the current consent state for user X?"
CREATE INDEX idx_consent_events_user_time    ON consent_events (user_id, occurred_at DESC);
CREATE INDEX idx_consent_events_consent_type ON consent_events (consent_type);

-- Rules (02_DATABASE_MVP.md §3.2):
-- • Append-only: no UPDATE or DELETE in application logic.
-- • Current consent = latest event per (user_id, consent_type) by occurred_at.
-- • Do NOT use MAX(policy_version) to determine latest consent.


-- ── audit_logs ─────────────────────────────────────────────────────────────
-- Generic audit log.  No raw chat or full sensitive content here.
-- Use the metadata JSONB column for structured event context.
CREATE TABLE audit_logs (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    category      VARCHAR(50) NOT NULL,
    action        VARCHAR(50) NOT NULL,
    actor_type    VARCHAR(20) NOT NULL,
    actor_id      UUID,
    subject_type  VARCHAR(50),
    subject_id    UUID,
    request_id    VARCHAR(100),
    metadata      JSONB,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT audit_logs_pkey PRIMARY KEY (id)
);

CREATE INDEX idx_audit_logs_actor        ON audit_logs (actor_id, created_at DESC);
CREATE INDEX idx_audit_logs_category     ON audit_logs (category);
CREATE INDEX idx_audit_logs_created_at   ON audit_logs (created_at);
