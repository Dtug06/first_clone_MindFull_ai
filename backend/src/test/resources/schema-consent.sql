-- H2-compatible DDL for the consent_events table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V3__create_consent_and_audit.sql.

CREATE TABLE IF NOT EXISTS consent_events (
    id             VARCHAR(36)  NOT NULL PRIMARY KEY,
    user_id        VARCHAR(36)  NOT NULL,
    consent_type   VARCHAR(50)  NOT NULL,
    action         VARCHAR(20)  NOT NULL,
    policy_version VARCHAR(50)  NOT NULL DEFAULT '1.0',
    metadata       TEXT,
    occurred_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT consent_type_check   CHECK (consent_type IN ('CHAT_ANALYSIS', 'PERSONALIZATION', 'EXPERT_SHARING')),
    CONSTRAINT consent_action_check CHECK (action        IN ('GRANTED', 'REVOKED')),
    CONSTRAINT consent_events_user_fk FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_consent_events_user_time ON consent_events (user_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_consent_events_consent_type ON consent_events (consent_type);