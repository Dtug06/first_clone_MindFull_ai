-- V18  G3-T12: Fixed Level 4 response templates
--
-- Stores the human-facing safety response content for Level 3 / Level 4
-- events, keyed by (locale, risk_reason). The SHOW_TEMPLATE executor (T12)
-- reads from this table directly  NEVER through any LLM call  so the
-- L4 path keeps working even if the AI provider is unavailable
-- (DoD  4.3 and docs/04_SAFETY_AND_CBT_RULES.md  3.4 "Khng s d ng free-form
-- LLM response", "S d ng fixed approved Safety Response").
--
-- Content governance (mirrors docs/04  15 +  27):
--   * Every row starts in status = DRAFT. Approval is gated by the
--     SafetyResponseTemplateService state machine:
--     DRAFT  PENDING_REVIEW  APPROVED  RETIRED.
--   * Status = APPROVED requires a non-null approved_by (FK to users.id
--     ON DELETE SET NULL) and approved_at. Only EXPERT/ADMIN roles may
--     approve (enforced in the service layer  same pattern as
--     SafetyKeywordRule.approve() in T08).
--   * Migrations DO NOT seed any row. The first template is inserted by
--     an expert via a future admin/back-office task (G7 or later).
--     Until then SHOW_TEMPLATE actions resolve to SKIPPED with a clear
--     reason, NEVER served an invented placeholder text. This honors
--     docs/04  27 "Khng t sng to crisis wording" / "Missing expert-approved
--     values must use TODO_EXPERT_REVIEW | CONFIG_PLACEHOLDER | DEMO_ONLY".
--   * Each (code, template_version) is unique. Each (code, locale,
--     risk_reason) may have at most one APPROVED row at any time (partial
--     unique index). At most one DEFAULT APPROVED row per locale (so the
--     fallback path is well-defined).
--
-- Locale check (MVP): only 'vi' is allowed. Adding 'en' / other locales is
-- a future migration once expert-authored content exists for that locale.
--
-- Risk-reason naming: matches the reason-code convention from
-- SafetyResolverService / docs/04  7 (UPPER_SNAKE_CASE). DEFAULT is a
-- sentinel value used only by is_default = TRUE fallback rows.
--
-- IMPORTANT invariants (intentionally NOT enforced at DB level because
-- PostgreSQL has no clean way to require "this row's content is the
-- latest approved version" without triggers):
--   * Every row has a (code, template_version); the service resolves
--     "latest APPROVED" via findFirstBy...OrderByTemplateVersionDesc.
--   * approved_by and approved_at are non-null IFF status = APPROVED
--     (enforced by the entity transitions, not by CHECK).

CREATE TABLE safety_response_templates (
    id                UUID         PRIMARY KEY,
    code              VARCHAR(100) NOT NULL,
    template_version  VARCHAR(50)  NOT NULL,
    locale            VARCHAR(10)  NOT NULL,
    risk_reason       VARCHAR(100) NOT NULL,
    content           TEXT         NOT NULL,
    is_default        BOOLEAN      NOT NULL DEFAULT FALSE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    approved_by       UUID         NULL REFERENCES users(id) ON DELETE SET NULL,
    approved_at       TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    lock_version      BIGINT       NULL,

    CONSTRAINT safety_response_templates_status_chk
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')),

    -- MVP locale whitelist. Add new locales via a future migration once an
    -- expert-authored template exists for that locale.
    CONSTRAINT safety_response_templates_locale_chk
        CHECK (locale IN ('vi')),

    -- Mirror reason-code naming convention from T09 / docs/04  7.
    -- Allowed "DEFAULT" sentinel for is_default = TRUE fallback rows.
    CONSTRAINT safety_response_templates_risk_reason_chk
        CHECK (risk_reason ~ '^[A-Z][A-Z0-9_]{1,99}$'),

    -- If status = APPROVED, both approved_by and approved_at must be set.
    -- Enforced here (cheap and read-only at the API/service boundary).
    CONSTRAINT safety_response_templates_approved_pair_chk
        CHECK (
            (status <> 'APPROVED')
            OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)
        ),

    -- A fallback row must carry is_default = TRUE; a specific row must carry
    -- is_default = FALSE. Avoids accidental "default row that isn't default".
    CONSTRAINT safety_response_templates_default_marker_chk
        CHECK (
            (risk_reason = 'DEFAULT' AND is_default = TRUE)
            OR (risk_reason <> 'DEFAULT' AND is_default = FALSE)
        )
);

-- One immutable (code, template_version). The entity treats this as the
-- natural key; we let the DB enforce it.
CREATE UNIQUE INDEX safety_response_templates_code_version_uq
    ON safety_response_templates (code, template_version);

-- Hot path for the executor: "give me the latest APPROVED row for
-- (locale, risk_reason)". A plain composite index is sufficient; the
-- application narrows to status = APPROVED via a JPQL predicate.
CREATE INDEX safety_response_templates_lookup_idx
    ON safety_response_templates (locale, risk_reason, status);

-- Hot path for the fallback lookup: "give me the latest APPROVED default
-- row for locale".
CREATE INDEX safety_response_templates_default_lookup_idx
    ON safety_response_templates (locale)
    WHERE is_default = TRUE;

-- Partial UNIQUE: at most one APPROVED row per (code, locale, risk_reason).
CREATE UNIQUE INDEX safety_response_templates_one_approved_per_triple_uq
    ON safety_response_templates (code, locale, risk_reason)
    WHERE status = 'APPROVED';

-- Partial UNIQUE: at most one APPROVED default row per locale.
CREATE UNIQUE INDEX safety_response_templates_one_default_per_locale_uq
    ON safety_response_templates (locale)
    WHERE is_default = TRUE AND status = 'APPROVED';
