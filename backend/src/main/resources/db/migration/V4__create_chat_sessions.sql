-- V4__create_chat_sessions.sql
-- MindBridge AI — G2-T01
-- chat_sessions: manages individual chat sessions per user.
-- Replaces self-FK on messages approach from early design.

CREATE TABLE chat_sessions (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL,
    title       VARCHAR(200),
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    closed_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chat_sessions_pkey             PRIMARY KEY (id),
    CONSTRAINT chat_sessions_user_fk          FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chat_sessions_status_check     CHECK (status IN ('ACTIVE', 'CLOSED', 'ARCHIVED'))
);

-- Primary access pattern: list sessions for a user ordered by most recently active.
CREATE INDEX idx_chat_sessions_user_updated
    ON chat_sessions (user_id, updated_at DESC);
