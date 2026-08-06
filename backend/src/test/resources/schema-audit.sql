-- H2-compatible DDL for the audit_logs table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V3__create_consent_and_audit.sql.

CREATE TABLE IF NOT EXISTS audit_logs (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    category     VARCHAR(50)  NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    actor_type   VARCHAR(20)  NOT NULL,
    actor_id     VARCHAR(36),
    subject_type VARCHAR(50),
    subject_id   VARCHAR(36),
    request_id   VARCHAR(100),
    metadata     TEXT,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor        ON audit_logs (actor_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_category     ON audit_logs (category);
CREATE INDEX IF NOT EXISTS idx_audit_logs_request_id   ON audit_logs (request_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at   ON audit_logs (created_at);