-- V8: Daily Question Assignments
-- Scope: G2-T05 — assign today's questionnaire to each user based on their local date.
--
-- Each row represents one (user, template version, local date) — i.e. one question
-- one user on one calendar day in their timezone.
--
-- Versioning: template_version_id FKs to daily_question_templates.id (each row is a
-- version, per G2-T04 versioning rule). Once assigned, the assignment preserves the
-- exact template version that was active at giao time — even if a newer version is
-- published later in the same day.
--
-- Local date: assigned_for_date is the wall-clock date in the user's timezone at the
-- time of assignment. timezone is denormalized so the original assignment context is
-- preserved even if the user later changes their timezone.
--
-- Uniqueness (per Phase 1 plan, option C):
--   - DB-level UNIQUE (user_id, template_version_id, assigned_for_date) acts as a
--     last-line safety net against duplicate inserts at the storage layer.
--   - Application-level guard at the service uses (user_id, template_code,
--     assigned_for_date) to decide whether to assign — this is the natural key
--     because template_code is version-independent.

CREATE TABLE daily_question_assignments (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL,
    template_version_id UUID         NOT NULL,
    template_code       VARCHAR(50)  NOT NULL,
    assigned_for_date   DATE         NOT NULL,
    timezone            VARCHAR(50)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ASSIGNED',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT daily_question_assignments_pkey PRIMARY KEY (id),
    CONSTRAINT daily_question_assignments_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT daily_question_assignments_template_version_fk
        FOREIGN KEY (template_version_id)
        REFERENCES daily_question_templates(id) ON DELETE RESTRICT,
    CONSTRAINT daily_question_assignments_unique
        UNIQUE (user_id, template_version_id, assigned_for_date),
    CONSTRAINT daily_question_assignments_status_check
        CHECK (status IN ('ASSIGNED', 'ANSWERED', 'SKIPPED', 'EXPIRED'))
);

-- Primary lookup: "today's assignments for user X"
CREATE INDEX idx_dqa_user_date ON daily_question_assignments (user_id, assigned_for_date);

-- Reverse lookup: "who has been assigned this template version?"
CREATE INDEX idx_dqa_template_version ON daily_question_assignments (template_version_id);

-- Service-level guard key: "has this user already been assigned template CODE for date?"
CREATE INDEX idx_dqa_user_code_date ON daily_question_assignments (user_id, template_code, assigned_for_date);
