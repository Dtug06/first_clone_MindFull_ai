package com.mindbridge.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for POST /auth/login.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a well-formed email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
