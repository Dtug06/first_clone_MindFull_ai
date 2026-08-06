package com.mindbridge.behavior.domain;

/**
 * Identifies which business table the event came from.
 *
 * Used together with {@code source_id} to trace an event back to its origin
 * row. {@code source_type} is informational only — no FK constraint to source
 * tables (events must survive deletion of the source row for historical
 * analysis).
 *
 * Per docs/02_DATABASE_MVP.md §4.7 + G2-T07 plan §2.6.
 */
public enum SourceType {
    CHAT_SESSION,
    CONVERSATION_MESSAGE,
    DAILY_QUESTION_ASSIGNMENT,
    DAILY_QUESTION_ANSWER
}