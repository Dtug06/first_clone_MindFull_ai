-- H2-compatible DDL for the user_behavior_profiles table.
-- Mirrors V23 migration + V24 (G4-T11 data_quality_status column).
--
-- Differences from PostgreSQL (V23):
--   * JSONB -> VARCHAR(8192). H2 does not have a native JSONB type.
--   * FK to risk_state_history is dropped.
--   * IF NOT EXISTS guards throughout.
CREATE TABLE IF NOT EXISTS user_behavior_profiles (
    id                          UUID            NOT NULL PRIMARY KEY,
    user_id                     UUID            NOT NULL,
    window_end                  DATE            NOT NULL,
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
    engagement_score_7d         INTEGER         NULL,
    engagement_score_30d        INTEGER         NULL,
    trend_summary               VARCHAR(8192)   NULL,
    dominant_topics_7d          VARCHAR(8192)   NOT NULL DEFAULT '[]',
    dominant_topics_30d         VARCHAR(8192)   NOT NULL DEFAULT '[]',
    risk_level                  SMALLINT        NULL,
    risk_history_id             UUID            NULL,
    data_coverage               NUMERIC(4, 3)   NOT NULL,
    confidence                  NUMERIC(4, 3)   NOT NULL,
    data_quality_status         VARCHAR(20)     NOT NULL DEFAULT 'INSUFFICIENT',
    profile_version             VARCHAR(50)     NOT NULL,
    calculation_version         VARCHAR(200)    NOT NULL,
    calculated_at               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_behavior_profiles_user_unique UNIQUE (user_id),
    CONSTRAINT user_behavior_profiles_engagement_7d_chk
        CHECK (engagement_score_7d IS NULL OR engagement_score_7d BETWEEN 0 AND 3),
    CONSTRAINT user_behavior_profiles_engagement_30d_chk
        CHECK (engagement_score_30d IS NULL OR engagement_score_30d BETWEEN 0 AND 3),
    CONSTRAINT user_behavior_profiles_risk_chk
        CHECK (risk_level IS NULL OR risk_level BETWEEN 1 AND 4),
    CONSTRAINT user_behavior_profiles_coverage_chk
        CHECK (data_coverage BETWEEN 0 AND 1),
    CONSTRAINT user_behavior_profiles_confidence_chk
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT user_behavior_profiles_data_quality_status_chk
        CHECK (data_quality_status IN ('SUFFICIENT', 'LOW', 'INSUFFICIENT')),
    CONSTRAINT user_behavior_profiles_profile_version_chk
        CHECK (profile_version <> ''),
    CONSTRAINT user_behavior_profiles_calc_version_chk
        CHECK (calculation_version <> '')
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_behavior_profiles_user_id
    ON user_behavior_profiles (user_id);
CREATE INDEX IF NOT EXISTS idx_user_behavior_profiles_calculated_at_desc
    ON user_behavior_profiles (calculated_at DESC);