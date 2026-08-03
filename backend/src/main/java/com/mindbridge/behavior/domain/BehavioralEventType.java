package com.mindbridge.behavior.domain;

/**
 * Behavioral event types, per docs/02_DATABASE_MVP.md §4.7.
 *
 * G2-T07 hooks 4 of these: {@link #CHAT_SESSION_STARTED},
 * {@link #CHAT_MESSAGE_SENT}, {@link #DAILY_CHECKIN_COMPLETED}, and
 * {@link #DAILY_CHECKIN_SKIPPED}.
 *
 * The other 8 are reserved for upcoming G3+ tasks (exercise / program /
 * recommendation flows). They are declared in the enum and listed in the DB
 * CHECK constraint so adding new hooks later does NOT require an ALTER.
 *
 * Convention: SCREAMING_SNAKE_CASE, "DOMAIN_ACTION_..." pattern.
 * Naming MUST match DB-MVP §4.7 exactly — not user-facing API conventions.
 */
public enum BehavioralEventType {
    CHAT_SESSION_STARTED,
    CHAT_MESSAGE_SENT,
    DAILY_CHECKIN_COMPLETED,
    DAILY_CHECKIN_SKIPPED,
    EXERCISE_ASSIGNED,
    EXERCISE_STARTED,
    EXERCISE_COMPLETED,
    EXERCISE_SKIPPED,
    PROGRAM_ACCEPTED,
    PROGRAM_PAUSED,
    RECOMMENDATION_OPENED,
    RECOMMENDATION_HELPFUL
}