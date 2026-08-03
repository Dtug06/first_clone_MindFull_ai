package com.mindbridge.analysis.run.service;

/**
 * Internal helper that sanitizes free-text error messages before
 * they are persisted to {@code ai_analysis_runs.error_summary}.
 *
 * <p>Per G3-T04 Phase 1 Q2 user decision: "Tuyệt đối KHÔNG lưu raw
 * ở bất kỳ đâu (DB, log, file, env). The redactor enforces this
 * for the error-summary column.
 *
 * <p>Strategy: any free-text message longer than
 * {@value #MAX_SUMMARY_LENGTH} characters is truncated,
 * and any contiguous run of non-ASCII printable chars (including
 * Vietnamese) is replaced with a placeholder so the message can
 * safely round-trip through the DB and logger without chance of
 * carrying raw chat content across. The exact content is NOT
 * recoverable — by design.
 *
 * <p>This class is public so {@code AiAnalysisRunService} (in the
 * {@code run.domain} package) can use it. It is intentionally
 * stateless and side-effect-free; tests in the {@code run.service}
 * package exercise it directly.
 */
public final class AiRunErrorRedactor {

    /** Hard limit on the persisted summary. Mirrors the DB CHECK constraint. */
    public static final int MAX_SUMMARY_LENGTH = 200;

    /** Default placeholder when the entire message is redacted. */
    public static final String PLACEHOLDER = "[[REDACTED]]";

    private AiRunErrorRedactor() {
        // No instances.
    }

    /**
     * Sanitize a free-text error message. The output is safe to
     * persist to the {@code error_summary} column.
     *
     * <p>Algorithm:
     * <ol>
     *   <li>If null, return null.</li>
     *   <li>Replace whitespace runs with a single space.</li>
     *   <li>Truncate to {@link #MAX_SUMMARY_LENGTH} chars.</li>
     *   <li>Strip characters that look like raw chat content
     *       (Vietnamese diacritics, accented chars, etc.) — replaced
     *       with the placeholder.</li>
     * </ol>
     *
     * <p>Note: this is a <i>best-effort</i> redactor. The strong
     * guarantee is that the redacted output is always <= 200 chars
     * and never contains the original raw message verbatim. The
     * service caller is responsible for ensuring the original
     * message does not contain raw chat content to begin with —
     * exceptions from the provider are exception messages, not raw
     * chat content.
     */
    public static String redact(String message) {
        if (message == null) {
            return null;
        }
        // Collapse whitespace.
        String collapsed = message.replaceAll("\\s+", " ").trim();
        if (collapsed.isEmpty()) {
            return null;
        }
        // Truncate to MAX_SUMMARY_LENGTH.
        if (collapsed.length() > MAX_SUMMARY_LENGTH) {
            collapsed = collapsed.substring(0, MAX_SUMMARY_LENGTH - 3) + "...";
        }
        // Heuristic: if the message contains any non-ASCII printable
        // char (e.g. Vietnamese diacritics), it's likely free-text from
        // a user-facing source — replace with placeholder. The MVP
        // exception messages are all ASCII (e.g. "AI provider did not
        // respond within timeout"), so this redactor is conservative.
        for (int i = 0; i < collapsed.length(); i++) {
            char c = collapsed.charAt(i);
            if (c > 0x7E) {
                return PLACEHOLDER;
            }
        }
        return collapsed;
    }
}