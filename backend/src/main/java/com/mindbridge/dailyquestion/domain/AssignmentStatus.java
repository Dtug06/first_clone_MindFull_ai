package com.mindbridge.dailyquestion.domain;

/**
 * Status of a daily question assignment.
 *
 * - ASSIGNED: assignment created, no answer yet.
 * - ANSWERED: the user has submitted an answer (via /daily-checkins/{id}/answer).
 * - SKIPPED: the user explicitly skipped the question.
 * - EXPIRED: the assignment window has passed without an answer (set by future cron / cleanup).
 */
public enum AssignmentStatus {
    ASSIGNED,
    ANSWERED,
    SKIPPED,
    EXPIRED
}
