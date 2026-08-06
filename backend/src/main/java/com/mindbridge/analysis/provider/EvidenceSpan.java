package com.mindbridge.analysis.provider;

/**
 * A pointer into the original (redacted) message that the provider cites as
 * evidence for a topic, emotion, intent, or signal. The raw substring is
 * replaced with a SHA-256 hex hash so that downstream storage and audit
 * never carry the original wording outside the chat message itself.
 *
 * <p>{@code start} and {@code end} are character offsets into
 * {@code ChatAnalysisInput.content()} and use the same convention as
 * {@link String#substring(int, int)}: {@code start} inclusive, {@code end}
 * exclusive.
 *
 * @param start    inclusive start offset, must be {@code >= 0}.
 * @param end      exclusive end offset, must be {@code > start}.
 * @param textHash SHA-256 hex digest of the substring (64 lowercase hex
 *                 chars). Never the raw substring.
 */
public record EvidenceSpan(int start, int end, String textHash) {

    /** Expected length of a SHA-256 hex digest. */
    public static final int SHA256_HEX_LENGTH = 64;

    public EvidenceSpan {
        if (start < 0) {
            throw new IllegalArgumentException("start must be >= 0");
        }
        if (end <= start) {
            throw new IllegalArgumentException("end must be > start");
        }
        if (textHash == null || textHash.length() != SHA256_HEX_LENGTH) {
            throw new IllegalArgumentException(
                    "textHash must be a " + SHA256_HEX_LENGTH + "-char SHA-256 hex digest");
        }
    }
}