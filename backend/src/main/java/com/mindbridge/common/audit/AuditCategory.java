package com.mindbridge.common.audit;

/**
 * Top-level category for an audit log entry.
 * Kept short and stable — values map to the {@code audit_logs.category} column.
 */
public enum AuditCategory {
    AUTH,
    CONSENT,
    USER,
    ADMIN,

    /**
     * Safety events and actions (G3-T11). Used to audit the creation
     * of every {@code SafetyEvent} (Level 3-4) and the actions
     * attached to it. Value maps to {@code audit_logs.category}; the
     * CHECK constraint is implicit (VARCHAR(50) accepts any
     * 50-char string, but consumers should restrict to this enum).
     */
    SAFETY
}