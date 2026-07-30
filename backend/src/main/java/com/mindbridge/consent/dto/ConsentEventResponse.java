package com.mindbridge.consent.dto;

import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a consent event is recorded.
 */
public record ConsentEventResponse(
        UUID id,
        ConsentType consentType,
        ConsentAction action,
        String policyVersion,
        Instant occurredAt
) {
}