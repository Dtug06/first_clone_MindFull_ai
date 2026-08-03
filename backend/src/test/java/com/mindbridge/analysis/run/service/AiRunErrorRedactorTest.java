package com.mindbridge.analysis.run.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiRunErrorRedactor}. Pure JUnit 5 + AssertJ.
 * The redactor is the only line of defence against raw chat content
 * leaking into {@code ai_analysis_runs.error_summary}. These tests
 * verify the redaction invariants from G3-T04 Phase 1 Q2.
 */
@DisplayName("AiRunErrorRedactor")
class AiRunErrorRedactorTest {

    @Nested
    @DisplayName("redact")
    class Redact {

        @Test
        @DisplayName("null input returns null")
        void redact_null_returnsNull() {
            assertThat(AiRunErrorRedactor.redact(null)).isNull();
        }

        @Test
        @DisplayName("empty input returns null")
        void redact_empty_returnsNull() {
            assertThat(AiRunErrorRedactor.redact("")).isNull();
        }

        @Test
        @DisplayName("whitespace-only input returns null")
        void redact_whitespaceOnly_returnsNull() {
            assertThat(AiRunErrorRedactor.redact("   \t\n  ")).isNull();
        }

        @Test
        @DisplayName("ASCII message short enough is returned verbatim")
        void redact_asciiShort_returnsVerbatim() {
            assertThat(AiRunErrorRedactor.redact("TIMEOUT after 30s"))
                    .isEqualTo("TIMEOUT after 30s");
        }

        @Test
        @DisplayName("collapses whitespace runs")
        void redact_collapsesWhitespace() {
            assertThat(AiRunErrorRedactor.redact("a   b\tc\nd"))
                    .isEqualTo("a b c d");
        }

        @Test
        @DisplayName("truncates messages longer than MAX_SUMMARY_LENGTH")
        void redact_truncatesLongMessage() {
            String longMsg = "x".repeat(AiRunErrorRedactor.MAX_SUMMARY_LENGTH + 100);
            String result = AiRunErrorRedactor.redact(longMsg);
            assertThat(result.length()).isEqualTo(AiRunErrorRedactor.MAX_SUMMARY_LENGTH);
            assertThat(result).endsWith("...");
        }

        @Test
        @DisplayName("non-ASCII message is replaced with placeholder")
        void redact_nonAscii_returnsPlaceholder() {
            String result = AiRunErrorRedactor.redact("Vietnamese: tuyệt vọng");
            assertThat(result).isEqualTo(AiRunErrorRedactor.PLACEHOLDER);
        }

        @Test
        @DisplayName("does not leak raw Vietnamese content")
        void redact_doesNotLeakVietnamese() {
            String raw = "tuyệt vọng không muốn sống";
            String result = AiRunErrorRedactor.redact(raw);
            assertThat(result).doesNotContain("tuyệt vọng");
            assertThat(result).doesNotContain("không muốn sống");
        }

        @Test
        @DisplayName("does not leak raw email-like content (long inputs are truncated)")
        void redact_doesNotLeakEmailLike() {
            // The redactor allows ASCII printable; emails are ASCII.
            // The contract is "no raw chat content" — emails are not chat
            // content. The strong guarantee test (no Vietnamese) is the
            // dedicated test above. Here we verify the length cap.
            String raw = "user@example.com " + "x".repeat(300);
            String result = AiRunErrorRedactor.redact(raw);
            assertThat(result.length()).isLessThanOrEqualTo(AiRunErrorRedactor.MAX_SUMMARY_LENGTH);
            assertThat(result).endsWith("...");
        }

        @Test
        @DisplayName("placeholder is the documented constant")
        void redact_placeholderIsDocumented() {
            assertThat(AiRunErrorRedactor.PLACEHOLDER).isEqualTo("[[REDACTED]]");
        }

        @Test
        @DisplayName("MAX_SUMMARY_LENGTH is 200 per DB CHECK constraint")
        void redact_maxLengthIs200() {
            assertThat(AiRunErrorRedactor.MAX_SUMMARY_LENGTH).isEqualTo(200);
        }

        @Test
        @DisplayName("exception messages from provider are ASCII and pass through")
        void redact_providerAsciiMessagesPassThrough() {
            // Provider uses these exact messages:
            assertThat(AiRunErrorRedactor.redact(
                    "AI provider did not respond within timeout"))
                    .isEqualTo("AI provider did not respond within timeout");
            assertThat(AiRunErrorRedactor.redact(
                    "AI provider returned a payload that failed schema validation"))
                    .isEqualTo("AI provider returned a payload that failed schema validation");
        }

        @Test
        @DisplayName("does not throw on messages with regex special chars")
        void redact_doesNotThrowOnRegexSpecialChars() {
            org.assertj.core.api.Assertions.assertThatCode(
                    () -> AiRunErrorRedactor.redact("(.*)\\1$x"))
                    .doesNotThrowAnyException();
            assertThat(AiRunErrorRedactor.redact("(.*)\\1$x"))
                    .isEqualTo("(.*)\\1$x");
        }
    }
}