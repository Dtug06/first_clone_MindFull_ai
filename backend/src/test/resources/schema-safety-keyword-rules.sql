-- H2-compatible DDL for the safety_keyword_rules table.
-- Mirrors V13 migration. Used by integration tests when Flyway is
-- disabled (test profile).
--
-- Differences from PostgreSQL:
--   * H2 has UUID support; we use the same UUID column type.
--   * TIMESTAMPTZ maps to TIMESTAMP WITH TIME ZONE in H2.

CREATE TABLE IF NOT EXISTS safety_keyword_rules (
    id                UUID         NOT NULL PRIMARY KEY,
    code              VARCHAR(100) NOT NULL,
    rule_version      VARCHAR(50)  NOT NULL,
    pattern           TEXT         NOT NULL,
    match_type        VARCHAR(20)  NOT NULL,
    preliminary_risk  SMALLINT     NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    approved_by       UUID         NULL,
    approved_at       TIMESTAMP    NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version      BIGINT       NULL,

    CONSTRAINT safety_keyword_rules_unique UNIQUE (code, rule_version),
    CONSTRAINT safety_keyword_rules_match_type CHECK (match_type IN ('KEYWORD', 'REGEX')),
    CONSTRAINT safety_keyword_rules_status CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')),
    CONSTRAINT safety_keyword_rules_risk CHECK (preliminary_risk BETWEEN 1 AND 4)
);

-- H2 supports partial indexes via WHERE; keep them aligned with V13.
CREATE INDEX IF NOT EXISTS safety_keyword_rules_approved_lookup
    ON safety_keyword_rules (code);
