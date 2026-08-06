-- V7: Add timezone column to users table.
-- Scope: G2-T05 — required for daily_question_assignments.assigned_for_date calculation.
-- Without a user timezone we cannot compute local_date for the daily check-in.
--
-- Default 'UTC' for existing rows so the migration is non-breaking.
-- IANA timezone strings (e.g. 'Asia/Ho_Chi_Minh') are validated at the application layer —
-- a CHECK constraint with a whitelist is intentionally avoided because the IANA tz database
-- has hundreds of entries and is updated periodically.

ALTER TABLE users
    ADD COLUMN timezone VARCHAR(50) NOT NULL DEFAULT 'UTC';
