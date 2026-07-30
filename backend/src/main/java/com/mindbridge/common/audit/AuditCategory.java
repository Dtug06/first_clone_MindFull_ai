package com.mindbridge.common.audit;

/**
 * Top-level category for an audit log entry.
 * Kept short and stable — values map to the {@code audit_logs.category} column.
 */
public enum AuditCategory {
    AUTH,
    CONSENT,
    USER,
    ADMIN
}