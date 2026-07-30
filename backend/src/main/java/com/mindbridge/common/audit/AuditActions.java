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

    private AuditActions() {
    }
}