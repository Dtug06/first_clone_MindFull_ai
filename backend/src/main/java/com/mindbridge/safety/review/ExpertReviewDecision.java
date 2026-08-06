package com.mindbridge.safety.review;

public enum ExpertReviewDecision {
    CONFIRM_RISK,
    DOWNGRADE_RISK,
    ESCALATE,
    NO_ACTION,
    CONTINUE_MONITORING,
    REQUEST_FOLLOWUP,
    DISMISS
}