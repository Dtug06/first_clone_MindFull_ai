package com.mindbridge.safety.event;

/**
 * Origin of a {@code SafetyEvent}. Mirrors
 * {@code docs/02_DATABASE_MVP.md} section 6.3 and
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} section 4 "Safety Input Sources".
 *
 * <p>The values map 1-to-1 with the CHECK constraint on
 * {@code safety_event_sources.source_type} (V17). G3-T11 ships only
 * {@link #CHAT_ANALYSIS} wired from the chat pipeline; the remaining
 * three will be wired by the daily-check-in, exercise, and program
 * assessment modules when those tasks land.
 *
 * <p>Per docs/04 section 4: "Every Safety Event must have at least one
 * source. It is not allowed to store only {@code risk_level = 4}
 * without storing the source and reason."
 */
public enum SafetyEventSourceType {

    /**
     * Source = a {@code conversation_messages} row that triggered the
     * resolver decision. Wired in G3-T11 chat pipeline.
     */
    CHAT_ANALYSIS,

    /**
     * Source = a {@code daily_question_answers} row. Wired by future
     * daily-check-in safety integration (post-G3-T11).
     */
    DAILY_ANSWER,

    /**
     * Source = an {@code exercise_submissions} row. Wired by future
     * exercise-safety integration (post-G3-T11).
     */
    EXERCISE_SUBMISSION,

    /**
     * Source = a {@code program_assessments} row. Wired by future
     * assessment-safety integration (post-G3-T11).
     */
    PROGRAM_ASSESSMENT
}