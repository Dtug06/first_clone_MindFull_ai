package com.mindbridge.common.audit;

/**
 * Who performed the action being audited.
 */
public enum AuditActorType {
    USER,
    EXPERT,
    ADMIN,
    ANONYMOUS,
    SYSTEM
}