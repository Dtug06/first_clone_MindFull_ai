-- V1__enable_extensions.sql
-- MindBridge AI — G1-T04
-- Enable PostgreSQL extensions required for the MVP schema.
-- IF NOT EXISTS makes this idempotent — safe to re-run.
-- Extensions must be enabled before any migration that uses them (V2, V3).

CREATE EXTENSION IF NOT EXISTS citext;
-- citext provides case-insensitive text type.
-- Used by: users.email
-- Indexes on citext columns behave as expected (no functional lower() wrapper needed).

CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- pgcrypto provides cryptographically secure random UUID generation.
-- Used by: gen_random_uuid() in V2 (users PK defaults).
-- Also available to application code for any future UUID needs.

-- vector extension is NOT enabled here.
-- pgvector is needed only when RAG is implemented (DEFERRED post-MVP).
-- Do not enable it in T04.
