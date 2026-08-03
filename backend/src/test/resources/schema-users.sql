-- H2-compatible DDL for the users table.
-- Used by integration tests when Flyway is disabled (test profile).
-- Must stay in sync with V2__create_users.sql + V7__add_user_timezone.sql.

CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email   ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_status  ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_role    ON users (role);
