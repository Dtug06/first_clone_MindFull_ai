package com.mindbridge.common.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LogSanitizer}.
 * Verifies deterministic, lowercase SHA-256 hex output and null-safety.
 */
@DisplayName("LogSanitizer")
class LogSanitizerTest {

    @Test
    @DisplayName("sha256Hex is deterministic and lowercase")
    void deterministicLowercase() {
        String first = LogSanitizer.sha256Hex("alice@example.com");
        String second = LogSanitizer.sha256Hex("ALICE@example.com");
        String third = LogSanitizer.sha256Hex("  alice@example.com ");

        assertThat(first).isEqualTo(second).isEqualTo(third);
        assertThat(first).matches("[0-9a-f]{64}");
    }

    @Test
    @DisplayName("Different inputs produce different hashes")
    void differentInputs() {
        assertThat(LogSanitizer.sha256Hex("a@b.com"))
                .isNotEqualTo(LogSanitizer.sha256Hex("c@d.com"));
    }

    @Test
    @DisplayName("Null returns null")
    void nullSafe() {
        assertThat(LogSanitizer.sha256Hex(null)).isNull();
    }
}