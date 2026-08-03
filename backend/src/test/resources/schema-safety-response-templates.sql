-- H2-compatible DDL for the safety_response_templates table.
-- Mirrors V18 migration. Used by integration tests when Flyway is disabled
-- (test profile).
--
-- Differences from PostgreSQL:
--   * TIMESTAMPTZ maps to TIMESTAMP (H2 in-memory mode is permissive and
--     round-trips OffsetDateTime; we use plain TIMESTAMP for portability
--     against H2).
--   * Partial UNIQUE indexes with predicate (WHERE ...) are NOT supported
--     on this H2 instance, so they are replaced with plain UNIQUE indexes
--     on (code, template_version). The "1 APPROVED per triple" / "1 default
--     per locale" invariants are enforced by application-layer checks in
--     SafetyResponseTemplateService.approve(...) and SafetyResponseTemplate
--     transition methods. This mirrors how schema-safety-events.sql handles
--     the equivalent trade-off for safety_events / safety_actions.
--   * is_default uses native BOOLEAN (mirrors PostgreSQL V18). The
--     earlier VARCHAR(1) 'Y'/'N' encoding was incompatible with the JPA
--     `boolean isDefault` field, which Hibernate serialises as TRUE/FALSE.
--     The CHECK constraint is rewritten to use 1/0 literals so the
--     application-layer validation in SafetyResponseTemplate.create()
--     stays correct.
--   * No FK to users.id - tests insert users rows out-of-band and we use
--     VARCHAR(36) user ids to match schema-users.sql.

CREATE TABLE IF NOT EXISTS safety_response_templates (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    code             VARCHAR(100) NOT NULL,
    template_version VARCHAR(50)  NOT NULL,
    locale           VARCHAR(10)  NOT NULL,
    risk_reason      VARCHAR(100) NOT NULL,
    content          CLOB         NOT NULL,
    is_default       BOOLEAN      NOT NULL DEFAULT FALSE,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    approved_by      VARCHAR(36)  NULL,
    approved_at      TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version     BIGINT       NULL,

    CONSTRAINT safety_response_templates_status_chk
        CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')),
    CONSTRAINT safety_response_templates_locale_chk
        CHECK (locale IN ('vi')),
    CONSTRAINT safety_response_templates_risk_reason_chk
        CHECK (risk_reason ~ '^[A-Z][A-Z0-9_]{1,99}$'),
    CONSTRAINT safety_response_templates_approved_pair_chk
        CHECK (
            (status <> 'APPROVED')
            OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)
        ),
    CONSTRAINT safety_response_templates_default_marker_chk
        CHECK (
            (risk_reason = 'DEFAULT' AND is_default = TRUE)
            OR (risk_reason <> 'DEFAULT' AND is_default = FALSE)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS safety_response_templates_code_version_uq
    ON safety_response_templates (code, template_version);

CREATE INDEX IF NOT EXISTS safety_response_templates_lookup_idx
    ON safety_response_templates (locale, risk_reason, status);

CREATE INDEX IF NOT EXISTS safety_response_templates_default_lookup_idx
    ON safety_response_templates (locale);

-- %% users table for FK / audit integration %%
-- Mirrors schema-users.sql. Required by
-- SafetyResponseTemplateServiceIntegrationTest which seeds expert +
-- admin + user accounts directly via JdbcTemplate (the role check is
-- package-private, so the test cannot go through User.setRole). The
-- production schema is owned by V2/V7; this H2 mirror is intentionally
-- independent to avoid coupling two test slices.
CREATE TABLE IF NOT EXISTS users (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    timezone      VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email   ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_status  ON users (status);
CREATE INDEX IF NOT EXISTS idx_users_role    ON users (role);