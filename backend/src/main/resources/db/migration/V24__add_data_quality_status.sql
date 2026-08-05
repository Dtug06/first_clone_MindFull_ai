-- V24 -- G4-T11: Add data_quality_status to user_behavior_profiles
ALTER TABLE user_behavior_profiles ADD COLUMN data_quality_status VARCHAR(20) NOT NULL DEFAULT 'INSUFFICIENT';
COMMENT ON COLUMN user_behavior_profiles.data_quality_status IS 'G4-T11 data quality status: SUFFICIENT | LOW | INSUFFICIENT. Derived from DataQualityConfig (TODO_EXPERT_REVIEW). G6 derives deferRequired internally.';
ALTER TABLE user_behavior_profiles ADD CONSTRAINT user_behavior_profiles_data_quality_status_chk CHECK (data_quality_status IN ('SUFFICIENT', 'LOW', 'INSUFFICIENT'));