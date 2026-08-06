package com.mindbridge.common.audit;

/**
 * Stable action codes for audit events.
 * Values must remain stable — they map to {@code audit_logs.action} and may
 * be used by analysts to filter or group events.
 */
public final class AuditActions {

    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String CONSENT_GRANTED = "CONSENT_GRANTED";
    public static final String CONSENT_REVOKED = "CONSENT_REVOKED";
    public static final String ROLE_CHANGED = "ROLE_CHANGED";
    public static final String ADMIN_ACTION = "ADMIN_ACTION";

    // G3-T11 — Safety events and actions (audit hook in SafetyEventService).
    public static final String SAFETY_EVENT_OPENED = "SAFETY_EVENT_OPENED";
    public static final String SAFETY_ACTION_RECORDED = "SAFETY_ACTION_RECORDED";

    // G3-T13 — Expert review (audit hooks in ExpertReviewService).
    /** A reviewer opened an event detail view. */
    public static final String EXPERT_REVIEW_OPENED = "EXPERT_REVIEW_OPENED";
    /** A reviewer submitted a decision on an event. */
    public static final String EXPERT_REVIEW_DECIDED = "EXPERT_REVIEW_DECIDED";

    private AuditActions() {
    }
}