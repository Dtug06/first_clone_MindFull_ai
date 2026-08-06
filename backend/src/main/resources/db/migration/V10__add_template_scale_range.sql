-- V10: Add scale range columns to daily_question_templates.
-- Scope: G2-T06 — required for answer validation. SCALE/NUMBER questions
-- must reject numeric values outside [scale_min, scale_max].
--
-- Two columns added:
--   scale_min NUMERIC NULL — inclusive lower bound; NULL means "no lower bound"
--   scale_max NUMERIC NULL — inclusive upper bound; NULL means "no upper bound"
--
-- Both nullable so non-numeric question types (SINGLE_CHOICE, TEXT) leave them NULL.
-- If both NULL → range validation is skipped for that template.
--
-- Backfill MVP seeds with their MVP ranges:
--   STRESS, ENERGY = 1-5 (1..5 SCALE)
--   SLEEP         = 0-24 (NUMBER, hours)
--   MOOD, OPEN    = NULL (SINGLE_CHOICE/TEXT — not applicable)

ALTER TABLE daily_question_templates
    ADD COLUMN scale_min NUMERIC,
    ADD COLUMN scale_max NUMERIC;

UPDATE daily_question_templates
SET scale_min = 1, scale_max = 5
WHERE code IN ('STRESS', 'ENERGY') AND version = 1;

UPDATE daily_question_templates
SET scale_min = 0, scale_max = 24
WHERE code = 'SLEEP' AND version = 1;