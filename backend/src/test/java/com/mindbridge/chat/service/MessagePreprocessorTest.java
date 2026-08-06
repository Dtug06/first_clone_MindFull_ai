package com.mindbridge.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.chat.exception.MessageValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for MessagePreprocessor.
 *
 * Scope: validate that validation and redaction behave correctly.
 * No logging is tested here — the no-logging guarantee is enforced by code
 * review of the service layer (Phase 3 audit).
 */
@DisplayName("MessagePreprocessor")
class MessagePreprocessorTest {

    private MessagePreprocessor preprocessor;

    @BeforeEach
    void setUp() {
        preprocessor = new MessagePreprocessor();
    }

    // --- Validation: blank / empty ---

    @Nested
    @DisplayName("Validation: empty content")
    class EmptyContent {

        @Test
        @DisplayName("null → MessageValidationException 400")
        void nullContent() {
            assertThatThrownBy(() -> preprocessor.process(null))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        @DisplayName("empty string → MessageValidationException 400")
        void emptyString() {
            assertThatThrownBy(() -> preprocessor.process(""))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("must not be empty");
        }

        @Test
        @DisplayName("whitespace only → MessageValidationException 400")
        void whitespaceOnly() {
            assertThatThrownBy(() -> preprocessor.process("   \t\n\r   "))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("must not be empty");
        }
    }

    // --- Validation: length ---

    @Nested
    @DisplayName("Validation: content length")
    class ContentLength {

        @Test
        @DisplayName("exactly 10 000 characters → accepted")
        void exactlyMaxLength() {
            String content = "a".repeat(10_000);
            String result = preprocessor.process(content);
            assertThat(result).hasSize(10_000);
        }

        @Test
        @DisplayName("10 001 characters → MessageValidationException 400")
        void exceedsMaxLength() {
            String content = "a".repeat(10_001);
            assertThatThrownBy(() -> preprocessor.process(content))
                    .isInstanceOf(MessageValidationException.class)
                    .hasMessageContaining("10000")
                    .hasMessageContaining("maximum length");
        }

        @Test
        @DisplayName("at boundary (10 000) → no exception")
        void boundary() {
            String content = "x".repeat(10_000);
            assertThat(preprocessor.process(content)).hasSize(10_000);
        }
    }

    // --- Redaction: email ---

    @Nested
    @DisplayName("Redaction: email")
    class EmailRedaction {

        @Test
        @DisplayName("simple email → replaced with [REDACTED-EMAIL]")
        void simpleEmail() {
            String result = preprocessor.process("Contact me at john.doe@example.com please");
            assertThat(result).isEqualTo("Contact me at [REDACTED-EMAIL] please");
        }

        @Test
        @DisplayName("multiple emails → all replaced")
        void multipleEmails() {
            String result = preprocessor.process(
                    "Email alice@test.com and bob@test.org at the same time");
            assertThat(result).isEqualTo(
                    "Email [REDACTED-EMAIL] and [REDACTED-EMAIL] at the same time");
        }

        @Test
        @DisplayName("no email → content unchanged")
        void noEmail() {
            String input = "Tôi cảm thấy mệt mỏi với công việc hiện tại";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("gmail-style email → replaced")
        void gmailStyle() {
            assertThat(preprocessor.process("bob@gmail.com"))
                    .isEqualTo("[REDACTED-EMAIL]");
        }

        @Test
        @DisplayName("email with subdomain → replaced")
        void emailWithSubdomain() {
            assertThat(preprocessor.process("admin@mail.mindbridge.ai"))
                    .isEqualTo("[REDACTED-EMAIL]");
        }

        @Test
        @DisplayName("email with dots and plus → replaced")
        void emailWithDotsAndPlus() {
            assertThat(preprocessor.process("john.doe+label@company.co.uk"))
                    .isEqualTo("[REDACTED-EMAIL]");
        }
    }

    // --- Unicode and multi-byte characters ---

    @Nested
    @DisplayName("Unicode handling")
    class UnicodeHandling {

        @Test
        @DisplayName("Vietnamese diacritics → preserved")
        void vietnameseDiacritics() {
            String input = "Tôi cảm thấy rất mệt mỏi và lo âu";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("emoji → preserved")
        void emoji() {
            String input = "Hôm nay vui quá! 😊🌟";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("mixed Vietnamese + emoji → preserved")
        void mixedVietnameseAndEmoji() {
            String input = "Tôi thấy 😢 buồn quá, cảm ơn bạn";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("CJK characters → preserved")
        void cjk() {
            String input = "今日は很开心 😊";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }
    }

    // --- Newlines and whitespace ---

    @Nested
    @DisplayName("Newline and whitespace handling")
    class WhitespaceHandling {

        @Test
        @DisplayName("multi-line content → newlines preserved")
        void multiLine() {
            String input = "Line 1\nLine 2\nLine 3";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("mixed whitespace → preserved")
        void mixedWhitespace() {
            String input = "  Leading\n\tMixed\r\nWhitespace  ";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("leading/trailing whitespace → preserved (not trimmed by processor)")
        void leadingTrailingPreserved() {
            String input = "  Significant leading spaces  ";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }
    }

    // --- Edge cases ---

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("content shorter than email pattern → no false positive")
        void shortNoFalsePositive() {
            String input = "ab@example";  // incomplete email, no TLD
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("phone number not redacted (phone out of G2-T03 scope)")
        void phoneNotRedacted() {
            String input = "Gọi tôi lúc 0912345678 nhé";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("9-digit number not redacted (ID out of G2-T03 scope)")
        void shortDigitSequenceNotRedacted() {
            // 9-digit sequences could be many things — ID redaction deferred to future task
            String input = "Số 123456789 không phải email";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("normal text with no PII → unchanged")
        void normalText() {
            String input = "I had a really tough day at work today.";
            assertThat(preprocessor.process(input)).isEqualTo(input);
        }
    }

    // --- isRedacted helper ---

    @Nested
    @DisplayName("isRedacted helper")
    class IsRedacted {

        @Test
        @DisplayName("true when content contains placeholder")
        void trueForPlaceholder() {
            assertThat(preprocessor.isRedacted("Contact at [REDACTED-EMAIL]")).isTrue();
        }

        @Test
        @DisplayName("false when content is clean")
        void falseForClean() {
            assertThat(preprocessor.isRedacted("Hello world")).isFalse();
        }

        @Test
        @DisplayName("false for null")
        void falseForNull() {
            assertThat(preprocessor.isRedacted(null)).isFalse();
        }
    }
}
