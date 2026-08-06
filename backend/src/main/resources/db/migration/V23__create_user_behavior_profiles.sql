-- V23 -- G4-T09: User Behavior Profile (current, mutable, one row per user)
--
-- One current profile per user. Profile API (T12) reads from this table;
-- G6 matching pipeline can read it as a "current read" snapshot.
--
-- Design rationale (G4-T09 Phase 1 plan, decisions confirmed by user):
--   * user_behavior_profiles  : mutable current profile (this migration).
--   * user_behavior_profile_snapshots (T10, DEFERRED) : immutable
--     append-only audit trail for each matching run (DBMVP §7.3, V_T10).
--     Snapshot table is INTENTIONALLY NOT created here - it will be a
--     separate Flyway migration under T10 scope if/when that task is
--     approved. Do NOT extend this table with snapshot/history semantics
--     - rule 30 ("Behavior Profile Snapshot used by Matching is immutable")
--     would then be silently violated.
--
-- Why mutable:
--   * Idempotent UPSERT (Section "INSERT UPSERT" below) handles the
--     "two jobs same user" race deterministically (last-write-wins by
--     calculated_at). No @Version JPA overhead, no retry logic.
--   * Job runs at 03:15 UTC every day (DailyFeatureAggregationJob at 03:00
--     + profile 15 min later). Backfill / CLI runners converge to the
--     same final row via the UPSERT WHERE clause.
--
-- Column selection:
--   * Typed columns (NUMERIC / INTEGER / DATE) for the 7d/30d metric
--     averages, risk_level, coverage and confidence - these are the
--     "frequently queried metrics" the behavior analysis rule
--     (30-database-ai-safety.mdc §"Do not use JSONB as a replacement for
--     every typed field") wants as typed.
--   * JSONB for trend_summary / dominant_topics_7d / dominant_topics_30d
--     because those are RENDER payloads (1:N arrays of nested objects
--     with no filter / aggregate / sort at query time). Typed expansion
--     would add ~10 nullable columns with no query benefit. The
--     "frequently queried metrics" exception applies.
--   * dominant_topics_7d / dominant_topics_30d store JSON arrays of
--     DominantTopic {topic, frequency, share} (the OpenAPI T08 schema).
--     NOT NULL with default '[]' so coverage/confidence-only reads
--     always work without null check.
--
-- Engagement score domain:
--   * engagement_score_7d / engagement_score_30d are INTEGER [0, 3]
--     (T08 binary count, calculation_version "engagement_v1_unweighted_top_n_3").
--     NOT [0, 1] like V21 column engagement_score (continuous ratio);
--     V21 column is intentionally NOT touched here.
--
-- Data quality columns:
--   * data_coverage : combined 7d+30d coverage fraction (max of the two
--     per T06 semantics; T06 returns both, this column stores their max
--     so the API can answer "is the profile informative yet?" without
--     computing in Java).
--   * confidence    : combined 7d+30d inferred-confidence fraction
--     (same max-of-two semantics).
--   * Both are NOT NULL with check constraint [0, 1] so the API can
--     always return them (DoD §3: "Coverage/confidence luon co trong
--     response, ke ca profile moi tao lan dau").
--   * For a brand-new user with no daily feature rows, the job writes
--     0.0 / 0.0 explicitly - profile *exists* but data is empty. This
--     matches the T05 "first day" semantics (UserDailyFeature row with
--     all-null scores still has explicit_coverage=0).
--
-- Calculation versions:
--   * profile_version  : semver for the profile shape itself. Bumped
--     when columns are added/renamed. MVP = "profile_v1".
--   * calculation_version : composite string that records which T05-T08
--     formula versions contributed. Format = "feat=<v>+trend=<v>+topic=<v>+eng=<v>".
--     Composite so ops can replay exactly what the profile was built
--     with at any past calculated_at.
--
-- Audit timestamps:
--   * created_at  : first insert (immutable in practice, but not enforced
--                   - this is a mutable table).
--   * calculated_at : last successful rebuild (used by the UPSERT
--                     monotonicity check).
--   * updated_at  : last write - useful for ops queries
--                   "which profiles have not been refreshed in N days?".
--
-- FK strategy:
--   * risk_history_id references risk_state_history(id) ON DELETE SET NULL
--     - a user may have their risk history row cleaned (append-only
--     retention) but the profile should not be deleted with it. Profile
--     itself stays; only risk_level goes back to NULL.
--   * No FK on user_id (intentionally). User deletion is CASCADE via
--     users.status='DELETED' (rule 30: app-level soft-delete, no DB-level
--     FK CASCADE on users because that would silently drop profiles
--     alongside users). If a user row is hard-deleted, the profile will
--     become orphan - we accept this risk for MVP scope (admin tooling
--     would clean up, but it is NOT a hot path). Documented; a future
--     retention task can add ON DELETE CASCADE if approved.
--
-- Indexes (hot paths):
--   * (user_id) UNIQUE - GET /api/v1/behavior/profile lookup by JWT
--     userId; UPSERT conflict target.
--   * (calculated_at DESC) - ops dashboard "profiles refreshed in last 24h"
--     + alert "profile not refreshed for >2 days".
--
-- Out of scope (later tasks):
--   * user_behavior_profile_snapshots (T10, DEFERRED).
--   * Cross-job correlation id (request_id, parent_job_run_id).
--   * Retention policy for old / never-refreshed profiles.

CREATE TABLE user_behavior_profiles (
    id                          UUID            PRIMARY KEY,

    -- 1 row / user (UPSERT key)
    user_id                     UUID            NOT NULL,

    -- 7d / 30d window anchor (last day of the windows)
    window_end                  DATE            NOT NULL,

    -- 7d / 30d metric averages (mirror T06 WindowAggregationResult)
    stress_avg_7d               NUMERIC(4, 3)   NULL,
    stress_avg_30d              NUMERIC(4, 3)   NULL,
    mood_avg_7d                 NUMERIC(4, 3)   NULL,
    mood_avg_30d                NUMERIC(4, 3)   NULL,
    energy_avg_7d               NUMERIC(4, 3)   NULL,
    energy_avg_30d              NUMERIC(4, 3)   NULL,
    sleep_avg_7d                NUMERIC(4, 3)   NULL,
    sleep_avg_30d               NUMERIC(4, 3)   NULL,
    anxiety_avg_7d              NUMERIC(4, 3)   NULL,
    anxiety_avg_30d             NUMERIC(4, 3)   NULL,

    -- engagement_score: INTEGER [0, 3] (T08 binary count, MVP v1-unweighted)
    engagement_score_7d         INTEGER         NULL,
    engagement_score_30d        INTEGER         NULL,

    -- Trends / streaks (T07 TrendSummary record, JSON-stringified)
    trend_summary               TEXT            NULL,

    -- Dominant topics (T08 DominantTopic array, JSON)
    dominant_topics_7d          JSONB           NOT NULL DEFAULT '[]'::jsonb,
    dominant_topics_30d         JSONB           NOT NULL DEFAULT '[]'::jsonb,

    -- Current risk (G3-T10 latest)
    risk_level                  SMALLINT        NULL,
    risk_history_id             UUID            NULL,

    -- Data quality
    data_coverage               NUMERIC(4, 3)   NOT NULL,
    confidence                  NUMERIC(4, 3)   NOT NULL,

    -- Versioning
    profile_version             VARCHAR(50)     NOT NULL,
    calculation_version         VARCHAR(200)    NOT NULL,

    -- Audit
    calculated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- 1 row / user (mutable, UPSERT by user_id)
    CONSTRAINT user_behavior_profiles_user_unique UNIQUE (user_id),

    -- FK on risk_history_id (ON DELETE SET NULL - see header)
    CONSTRAINT user_behavior_profiles_risk_history_fk
        FOREIGN KEY (risk_history_id)
        REFERENCES risk_state_history(id)
        ON DELETE SET NULL,

    -- Domain CHECKs
    CONSTRAINT user_behavior_profiles_engagement_7d_chk
        CHECK (engagement_score_7d IS NULL
               OR engagement_score_7d BETWEEN 0 AND 3),
    CONSTRAINT user_behavior_profiles_engagement_30d_chk
        CHECK (engagement_score_30d IS NULL
               OR engagement_score_30d BETWEEN 0 AND 3),
    CONSTRAINT user_behavior_profiles_risk_chk
        CHECK (risk_level IS NULL OR risk_level BETWEEN 1 AND 4),
    CONSTRAINT user_behavior_profiles_coverage_chk
        CHECK (data_coverage BETWEEN 0 AND 1),
    CONSTRAINT user_behavior_profiles_confidence_chk
        CHECK (confidence BETWEEN 0 AND 1),

    -- Non-blank versions
    CONSTRAINT user_behavior_profiles_profile_version_chk
        CHECK (profile_version <> ''),
    CONSTRAINT user_behavior_profiles_calc_version_chk
        CHECK (calculation_version <> '')
);

CREATE UNIQUE INDEX idx_user_behavior_profiles_user_id
    ON user_behavior_profiles (user_id);
CREATE INDEX idx_user_behavior_profiles_calculated_at_desc
    ON user_behavior_profiles (calculated_at DESC);