package com.mindbridge.auth.dto;

import com.mindbridge.user.dto.UserResponse;

/**
 * Response returned after a successful register or login.
 * Contains the JWT access token and the authenticated user summary.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {

    public AuthResponse(String accessToken, String tokenType, Long expiresIn, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType != null ? tokenType : "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }
}
