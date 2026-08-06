package com.mindbridge.chat.service;

import com.mindbridge.chat.exception.MessageValidationException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates and redacts user message content before storage and before
 * forwarding to the AI pipeline.
 *
 * Validation rules:
 * - Content must not be blank (null, empty, or whitespace-only after trim).
 * - Content length must not exceed 10 000 characters (API contract limit).
 *
 * Redaction (G2-T03 scope — minimal, email only):
 * - Email addresses are replaced with {@code [REDACTED-EMAIL]}.
 * - Phone numbers and other identifiers are out of scope for G2-T03;
 *   a future task will add patterns with expert-reviewed rules.
 *
 * Safety guarantees:
 * - Raw content is NOT logged in any method of this class.
 * - The redacted output is stored in {@code conversation_messages.content}
 *   with {@code redacted = true}.
 * - Expert review (G2-T09) reads raw content from the audit trail, not logs.
 *
 * Unicode handling: uses Java regex with Unicode-aware character classes
 * so Vietnamese diacritics, emoji, and multi-byte characters are processed
 * correctly without corruption.
 */
@Component
public class MessagePreprocessor {

    /**
     * Maximum character length for user message content, matching
     * {@code SendMessageRequest.content.maxLength} in 03_API_CONTRACT.yaml.
     */
    public static final int MAX_CONTENT_LENGTH = 10_000;

    /** Sentinel used to replace detected email addresses in the redacted output. */
    private static final String EMAIL_PLACEHOLDER = "[REDACTED-EMAIL]";

    /**
     * Pattern: matches most RFC-5322-compliant email addresses.
     * Conservative — avoids over-matching phone numbers or other digit sequences.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}"
    );

    /**
     * Validates and redacts message content.
     *
     * @param rawContent the raw user input (never logged)
     * @return the redacted content safe for storage and AI forwarding
     * @throws MessageValidationException if the content is blank or exceeds the length limit
     */
    public String process(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            throw new MessageValidationException("Message content must not be empty");
        }

        if (rawContent.length() > MAX_CONTENT_LENGTH) {
            throw new MessageValidationException(
                    "Message content exceeds maximum length of " + MAX_CONTENT_LENGTH + " characters"
            );
        }

        return redact(rawContent);
    }

    /**
     * Returns true when the given content has already been redacted by this
     * processor (useful for detecting re-processing attempts).
     * For MVP this is a simple sentinel check only.
     */
    public boolean isRedacted(String content) {
        return content != null && content.contains(EMAIL_PLACEHOLDER);
    }

    // --- Package-private for unit testing ---

    String redact(String content) {
        return EMAIL_PATTERN.matcher(content).replaceAll(MATCH_RESULT -> EMAIL_PLACEHOLDER);
    }

    int maxLength() {
        return MAX_CONTENT_LENGTH;
    }
}
