package com.mindbridge.safety.classifier;

import java.util.UUID;

/**
 * Input contract for {@link RiskClassifierProvider#classify(RiskClassifierInput)}.
 *
 * <p>Wraps the same four minimum fields used by
 * {@code com.mindbridge.analysis.provider.ChatAnalysisInput} (G3-T01):
 * message id, owning user id, redacted message content, and BCP-47
 * locale. The DTO is intentionally NOT a subclass of
 * {@code ChatAnalysisInput} — it is a separate safety-domain contract
 * that happens to share the same field set. This keeps the two
 * pipelines independent: a future change to chat analysis input (e.g.
 * adding conversation history) does not silently widen the risk
 * classifier input, and a future safety-specific field (e.g. current
 * risk state) can be added here without affecting chat analysis.
 *
 * <p>The {@code content} field is the redacted content produced by
 * {@code MessagePreprocessor}, never the raw user input. Providers
 * must not assume otherwise and must not log it (per
 * {@code .cursor/rules/30-database-ai-safety.mdc} Safety Rules and
 * §3.4 "Không sử dụng free-form LLM response").
 *
 * <p><b>Caller scope</b>: this DTO is consumed only by the Safety
 * Resolver (G3-T10). The risk classifier provider MUST NOT receive
 * any assistant response from the chat pipeline — per
 * {@code docs/tasks/G3/G3-T09-llm-risk-classification-rieng.md}
 * "không dùng assistant response làm classifier". The Safety Resolver
 * is expected to pass the user's message only.
 *
 * @param messageId the id of the persisted {@code conversation_messages}
 *                  row, used for traceability into the AI analysis run.
 * @param userId    the owning user id, taken from the JWT principal.
 *                  Used downstream for ownership checks; never trusted
 *                  for identity here.
 * @param content   the message content to classify. Must be non-blank
 *                  and at most {@link #MAX_CONTENT_LENGTH} characters.
 * @param locale    BCP-47 tag for the content language (e.g.
 *                  {@code vi-VN}). Defaults to {@code vi-VN}.
 */
public record RiskClassifierInput(
        UUID messageId,
        UUID userId,
        String content,
        String locale
) {
    /** Default locale when callers do not provide one. */
    public static final String DEFAULT_LOCALE = "vi-VN";

    /** Maximum content length, matching the chat analysis input contract. */
    public static final int MAX_CONTENT_LENGTH = 10_000;

    public RiskClassifierInput {
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
