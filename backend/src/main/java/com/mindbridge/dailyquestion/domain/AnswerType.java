package com.mindbridge.dailyquestion.domain;

/**
 * Type discriminator for daily question answers.
 *
 * Corresponds to DailyAnswerType in docs/03_API_CONTRACT.yaml.
 * Maps from QuestionType as follows:
 *   SCALE, NUMBER       → NUMERIC
 *   SINGLE_CHOICE       → OPTION
 *   TEXT                → TEXT
 *
 * Stored as VARCHAR(20) with a DB-level CHECK constraint (V9).
 */
public enum AnswerType {
    NUMERIC,
    TEXT,
    OPTION
}