package com.mindbridge.analysis.provider.pipeline;

/**
 * Pure decision function for whether the retry/fallback pipeline should
 * fall back to the mock provider. Kept separate from
 * {@link ProviderRetryExecutor} so the policy is unit-testable without
 * touching Spring or HTTP plumbing.
 *
 * <p>Policy (locked at G3-T07):
 *
 * <ul>
 *   <li>If retries are NOT exhausted Ã¢â€ â€™ NO fallback (the next attempt is
 *       still in scope).</li>
 *   <li>If retries ARE exhausted AND fallback is enabled Ã¢â€ â€™ fallback is
 *       allowed (subject to the {@code modelRiskLevel = 4} guard
 *       below).</li>
 *   <li>If fallback is disabled Ã¢â€ â€™ NEVER fallback.</li>
 *   <li>If the last primary attempt returned (or would have returned)
 *       a successful response with {@code modelRiskLevel = 4} on the
 *       classifier pipeline Ã¢â€ â€™ NEVER fallback (rule "Do not silently
 *       downgrade a model risk signal" Ã¢â‚¬â€
 *       {@code .cursor/rules/30-database-ai-safety.mdc} Ã‚Â§Safety
 *       Rules).</li>
 * </ul>
 *
 * <p>For the chat-analysis pipeline, the {@code modelRiskLevel} guard
 * is not enforced (chat analysis never reaches the resolver directly
 * Ã¢â‚¬â€ only the classifier does). Callers pass {@code -1} for "not
 * applicable".
 */
public final class FallbackDecision {

    private FallbackDecision() {
        // Pure function Ã¢â‚¬â€ no instances.
    }

    /**
     * @param retriesExhausted       true if the configured retry budget
     *                               was spent without success.
     * @param fallbackEnabled        the
     *                               {@link ProviderRetryProperties.Fallback#isEnabled()}
     *                               value for the active profile.
     * @param modelRiskLevelFromLastAttempt the risk level of the last
     *                                     attempted response (-1 if
     *                                     not applicable, e.g. for
     *                                     chat analysis or when no
     *                                     successful response was
     *                                     produced).
     * @return true if the executor should invoke the fallback provider.
     */
    public static boolean shouldFallback(
            boolean retriesExhausted,
            boolean fallbackEnabled,
            int modelRiskLevelFromLastAttempt) {

        if (!retriesExhausted) {
            return false;
        }
        if (!fallbackEnabled) {
            return false;
        }
        if (modelRiskLevelFromLastAttempt == 4) {
            // Safety guard: never mask a Level 4 signal with a mock.
            return false;
        }
        return true;
    }
}
