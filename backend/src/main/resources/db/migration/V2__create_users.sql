-- V2__create_users.sql
-- MindBridge AI — G1-T04
-- Identity table: one row per registered user account.
-- References: consent_events, audit_logs, all other tables in later migrations.

CREATE TABLE users (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    email         CITEXT      NOT NULL,
    password_hash TEXT        NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT users_pkey             PRIMARY KEY (id),
    CONSTRAINT users_email_unique      UNIQUE (email),
    CONSTRAINT users_role_check        CHECK (role   IN ('USER', 'EXPERT', 'ADMIN')),
    CONSTRAINT users_status_check      CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
);

-- citext (enabled in V1) is case-insensitive; the index below
-- is B-tree over citext and uses the efficient equality lookup.
CREATE INDEX idx_users_email   ON users (email);
CREATE INDEX idx_users_status  ON users (status);
CREATE INDEX idx_users_role    ON users (role);

-- Keep updated_at accurate; updated by application on every mutation.
-- No trigger here — application is responsible for setting updated_at.
