-- V13 — G3-T08: Safety keyword/regex rule storage
--
-- Stores versioned rules used by the safety pre-filter (Keyword/Regex
-- Pre-filter). Rules are versioned and must be APPROVED by an expert
-- before the pre-filter evaluates them. Status values mirror the CBT
-- content versioning flow described in
-- docs/04_SAFETY_AND_CBT_RULES.md §15 (DRAFT / PENDING_REVIEW / APPROVED
-- / RETIRED).
--
-- IMPORTANT: per docs/04_SAFETY_AND_CBT_RULES.md §6, "Cursor không được
-- tự tạo danh sách keyword production" — this migration creates the
-- schema only and does NOT seed any production rule. The pre-filter
-- returns a no-signal result until an expert inserts an APPROVED row.
-- No row seeded.

CREATE TABLE safety_keyword_rules (
    id                UUID         PRIMARY KEY,
    code              VARCHAR(100) NOT NULL,
    rule_version      VARCHAR(50)  NOT NULL,
    pattern           TEXT         NOT NULL,
    match_type        VARCHAR(20)  NOT NULL,
    preliminary_risk  SMALLINT     NOT NULL,
    status            VARCHAR(20)  NOT NULL,
    approved_by       UUID         NULL REFERENCES users(id) ON DELETE SET NULL,
    approved_at       TIMESTAMPTZ  NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version      BIGINT       NULL,

    CONSTRAINT safety_keyword_rules_unique     UNIQUE (code, rule_version),
    CONSTRAINT safety_keyword_rules_match_type CHECK (match_type IN ('KEYWORD', 'REGEX')),
    CONSTRAINT safety_keyword_rules_status     CHECK (status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')),
    CONSTRAINT safety_keyword_rules_risk       CHECK (preliminary_risk BETWEEN 1 AND 4)
);

-- At most one APPROVED row per code at any time. Enforced via partial
-- unique index — supports the "create new version + retire old" flow
-- without a serializable transaction.
CREATE UNIQUE INDEX safety_keyword_rules_one_active_per_code
    ON safety_keyword_rules (code)
    WHERE status = 'APPROVED';

-- Hot-path index for the pre-filter: "load every APPROVED rule". A
-- partial index keeps this cheap regardless of how many historical
-- versions exist.
CREATE INDEX safety_keyword_rules_approved_lookup
    ON safety_keyword_rules (code)
    WHERE status = 'APPROVED';
