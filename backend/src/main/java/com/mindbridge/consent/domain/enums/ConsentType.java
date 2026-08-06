package com.mindbridge.consent.domain.enums;

/**
 * Type of consent a user can grant or revoke.
 * Must match the CHECK constraint on consent_events.consent_type.
 */
public enum ConsentType {
    CHAT_ANALYSIS,
    PERSONALIZATION,
    EXPERT_SHARING
}