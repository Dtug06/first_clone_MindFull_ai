package com.mindbridge.safety.dto;

import java.util.List;
import java.util.UUID;

/**
 * Input contract for {@code SafetyPreFilterService.evaluate(...)}.
 *
 * <p>The {@code content} field is the REDACTED content produced by the
 * existing message preprocessor pipeline (G2-T03). The pre-filter
 * never sees raw user input — only the redacted form. This keeps the
 * data flow consistent with the AI Analysis contract.
 *
 * @param messageId the id of the persisted {@code conversation_messages}
 *                  row, used for traceability into the safety run.
 * @param userId    the owning user id, taken from the JWT principal.
 *                  Used for ownership checks in the consumer pipeline,
 *                  never for trust in this service.
 * @param content   the message content to evaluate. Must be non-blank
 *                  and at most {@link #MAX_CONTENT_LENGTH} characters.
 * @param locale    BCP-47 tag for the content language. Currently
 *                  informational — the pre-filter is locale-agnostic in
 *                  v1 because rules are stored per-code, not per-locale.
 *                  Reserved for v2.
 */
public record PreFilterInput(
        UUID messageId,
        UUID userId,
        String content,
        String locale
) {
    /** Maximum content length, matching the AI analysis contract. */
    public static final int MAX_CONTENT_LENGTH = 10_000;

    /** Default locale when callers do not provide one. */
    public static final String DEFAULT_LOCALE = "vi-VN";

    public PreFilterInput {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId must not be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be null or blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "content exceeds maximum length of " + MAX_CONTENT_LENGTH);
        }
        if (locale == null || locale.isBlank()) {
            locale = DEFAULT_LOCALE;
        }
    }

    /** Compact constructor shared factory for tests and helpers. */
    public static PreFilterInput of(UUID userId, String content) {
        return new PreFilterInput(UUID.randomUUID(), userId, content, DEFAULT_LOCALE);
    }

    /**
     * Convenience for unit tests that only care about content shape.
     */
    public static List<PreFilterInput> none() {
        return List.of();
    }
}
