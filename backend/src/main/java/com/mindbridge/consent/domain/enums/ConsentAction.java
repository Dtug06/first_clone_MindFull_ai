package com.mindbridge.consent.domain.enums;

/**
 * Action applied to a consent type.
 * Must match the CHECK constraint on consent_events.action.
 */
public enum ConsentAction {
    GRANTED,
    REVOKED
}