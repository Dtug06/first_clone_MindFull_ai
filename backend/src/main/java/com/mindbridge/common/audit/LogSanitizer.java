package com.mindbridge.common.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helpers for keeping sensitive values out of logs and audit rows.
 *
 * Email addresses are persisted as a SHA-256 hash. The hash is sufficient
 * to correlate repeated failures from the same user across audit rows
 * without storing the raw email.
 */
public final class LogSanitizer {

    private LogSanitizer() {
    }

    /**
     * Returns the lowercase SHA-256 hex digest of {@code value}.
     */
    public static String sha256Hex(String value) {
        if (value == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.toLowerCase().trim()
                    .getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}