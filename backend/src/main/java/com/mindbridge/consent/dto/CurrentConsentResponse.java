package com.mindbridge.consent.dto;

import com.mindbridge.consent.domain.enums.ConsentType;
import java.time.Instant;

/**
 * Response representing the current consent state for a consent type.
 */
public record CurrentConsentResponse(
        ConsentType consentType,
        boolean granted,
        String policyVersion,
        Instant updatedAt
) {
}