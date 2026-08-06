package com.mindbridge.analysis.provider;

/**
 * Per-call context that {@link com.mindbridge.analysis.provider.impl.RealLlmChatAnalysisProvider}
 * sets so the calling service ({@code AiAnalysisRunService}) can persist
 * the ACTUAL provider/model string returned by the upstream LLM, rather
 * than the static configured labels.
 *
 * <p><b>Why ThreadLocal?</b> The {@link ChatAnalysisProvider} contract is
 * {@code analyze(ChatAnalysisInput) -> ChatAnalysisOutput}. Adding a
 * second parameter or a return value just to surface the response-time
 * {@code model} string would either change the shared interface (touches
 * T01's mock, breaks tests) or hide state in the {@code ChatAnalysisOutput}
 * (a DTO that is supposed to describe the user message, not provider
 * metadata). A small per-thread stash keeps the interface untouched and
 * the override opt-in — only providers that care about surfacing
 * response metadata populate it.
 *
 * <p><b>Thread scope.</b> ThreadLocal only. The value must be cleared in
 * a {@code finally} block by whoever first calls {@link #set(Snapshot)};
 * the run service wraps the call in such a block. Mocks ({@code
 * MockChatAnalysisProvider}, the default) never call {@link #set}, so
 * {@link #current()} returns {@code null} and the existing behaviour is
 * preserved unchanged.
 *
 * <p><b>Contract</b> (only the run service is allowed to {@link
 * #current()} read; only the real provider is expected to {@link
 * #set(Snapshot)} write):
 * <ul>
 *   <li>{@code provider} — the upstreams response provider string (e.g.
 *       {@code "openai"}). Non-null, non-blank, max 50 chars.</li>
 *   <li>{@code model} — the literal model identifier returned by the
 *       provider (e.g. {@code "gpt-4o-2024-08-06"}). Never truncated
 *       or normalised; per Q6 the persisted value is verbatim for
 *       audit fidelity. Non-null, non-blank, max 100 chars.</li>
 * </ul>
 */
public final class RealLlmResponseContext {

    /**
     * Stored snapshot. Both fields are non-null/non-blank when present.
     *
     * @param provider provider identifier, max 50 chars
     * @param model    model identifier, max 100 chars
     */
    public record Snapshot(String provider, String model) {
        public Snapshot {
            if (provider == null || provider.isBlank()) {
                throw new IllegalArgumentException("provider must not be null or blank");
            }
            if (model == null || model.isBlank()) {
                throw new IllegalArgumentException("model must not be null or blank");
            }
            if (provider.length() > 50) {
                throw new IllegalArgumentException(
                        "provider exceeds max length 50 (was " + provider.length() + ")");
            }
            if (model.length() > 100) {
                throw new IllegalArgumentException(
                        "model exceeds max length 100 (was " + model.length() + ")");
            }
        }
    }

    private static final ThreadLocal<Snapshot> CURRENT = new ThreadLocal<>();

    private RealLlmResponseContext() {
        // No instances.
    }

    /**
     * Set the snapshot for the current thread. Replaces any prior value.
     * The caller is responsible for {@link #clear()} in a {@code finally}.
     */
    public static void set(Snapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("snapshot must not be null");
        }
        CURRENT.set(snapshot);
    }

    /**
     * Read the snapshot for the current thread, or {@code null} if no
     * provider has populated it. Returns {@code null} for the mock
     * provider path.
     */
    public static Snapshot current() {
        return CURRENT.get();
    }

    /**
     * Clear the snapshot for the current thread. Idempotent.
     */
    public static void clear() {
        CURRENT.remove();
    }
}
