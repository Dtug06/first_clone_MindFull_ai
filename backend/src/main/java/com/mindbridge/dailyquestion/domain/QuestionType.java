package com.mindbridge.dailyquestion.domain;

/**
 * Type of daily question, determining how users answer.
 *
 * Corresponds to DailyQuestionType in 03_API_CONTRACT.yaml.
 * SCALE and NUMBER render a numeric input; SINGLE_CHOICE renders a select
 * backed by daily_question_options; TEXT renders a free-text input.
 */
public enum QuestionType {
    SCALE,
    SINGLE_CHOICE,
    TEXT,
    NUMBER
}
