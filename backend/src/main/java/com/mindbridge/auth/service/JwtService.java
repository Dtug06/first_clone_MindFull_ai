package com.mindbridge.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles JWT access-token signing, verification and claims extraction.
 *
 * Security rules enforced here:
 * - Secret is read from environment (never hard-coded).
 * - Tokens carry only userId + role — never carry sensitive data.
 * - Token validation covers signature and expiry only.
 *
 * This service does NOT log tokens or passwords.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must be at least 32 characters (256 bits) for HS256. " +
                    "Current length: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for the given user.
     *
     * @param userId the user's UUID
     * @param email  the user's email (stored as subject)
     * @param role   the user's role string
     * @return a signed JWT string
     */
    public String generateAccessToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = Instant.ofEpochMilli(now.toEpochMilli() + accessTokenExpirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Returns true if the token is structurally valid and has not expired.
     * Returns false for tampered, malformed, or expired tokens.
     *
     * Never throws to callers — exceptions are swallowed and return false.
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log only that validation failed — never log the token itself
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the userId (subject) from a valid token.
     * Callers MUST call {@link #isTokenValid(String)} first.
     *
     * @throws JwtException if the token is not valid
     */
    public UUID extractUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the role claim from a valid token.
     */
    public String extractRole(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("role", String.class);
    }

    /**
     * Returns the configured token validity window in milliseconds.
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
