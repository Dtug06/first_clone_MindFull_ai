package com.mindbridge.analysis.provider;

import java.util.UUID;

/**
 * Input contract for {@link ChatAnalysisProvider#analyze(ChatAnalysisInput)}.
 *
 * <p>Only the minimum fields required by G3-T01 are present. Later tasks
 * (G3-T02 schema expansion, G3-T03 prompt) may add {@code previousTopic},
 * {@code locale}-specific template hints, or conversation history. Add
 * fields here only when a downstream consumer needs them — keeping the
 * contract small avoids forcing every provider to support every future
 * field.
 *
 * <p>Note: the {@code content} field is the redacted content produced by
 * {@code MessagePreprocessor}, never the raw user input. Providers must
 * not assume otherwise and must not log it.
 *
 * @param messageId the id of the persisted {@code conversation_messages}
 *                  row, used for traceability into the AI analysis run.
 * @param userId    the owning user id, taken from the JWT principal — used
 *                  for ownership checks downstream, never for trust.
 * @param content   the message content to analyse. Must be
 *                  {@link String#isBlank()} = {@code false}.
 * @param locale    BCP-47 tag for the content language (e.g. {@code vi-VN}).
 *                  Default {@code vi-VN}. Providers may use it to pick a
 *                  prompt variant.
 */
public record ChatAnalysisInput(
        UUID messageId,
        UUID userId,
        String content,
        String locale
) {
    /** Default locale when callers do not provide one. */
    public static final String DEFAULT_LOCALE = "vi-VN";

    /** Maximum content length, matching the API contract. */
    public static final int MAX_CONTENT_LENGTH = 10_000;

    /**
     * Compact constructor with minimal validation. Provider implementations
     * are expected to enforce their own invariants; this constructor only
     * guards against silent NPEs from null ids.
     */
    public ChatAnalysisInput {
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
}