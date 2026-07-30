package com.mindbridge.consent.dto;

import com.mindbridge.consent.domain.enums.ConsentAction;
import com.mindbridge.consent.domain.enums.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for POST /consents.
 */
public record ConsentEventRequest(
        @NotNull(message = "Consent type is required")
        ConsentType consentType,

        @NotNull(message = "Action is required")
        ConsentAction action,

        @NotBlank(message = "Policy version is required")
        String policyVersion
) {
}