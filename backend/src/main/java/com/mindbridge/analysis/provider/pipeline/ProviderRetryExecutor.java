package com.mindbridge.analysis.provider.pipeline;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException;
import com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException;
import com.mindbridge.safety.classifier.exception.RiskClassifierUnavailableException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bounded retry helper for AI provider calls.
 *
 * <p>Shared by {@link com.mindbridge.analysis.provider.ChatAnalysisProvider}
 * (chat analysis) and
 * {@link com.mindbridge.safety.classifier.RiskClassifierProvider} (Safety
 * pipeline) so the policy is consistent across both pipelines and lives
 * in one place.
 *
 * <p>Policy (locked at G3-T07):
 *
 * <ul>
 *   <li><b>Retryable</b>: {@code ProviderTimeoutException},
 *       {@code RiskClassifierTimeoutException},
 *       {@code ProviderUnavailableException},
 *       {@code RiskClassifierUnavailableException}.</li>
 *   <li><b>Non-retryable</b>: {@code InvalidAnalysisOutputException},
 *       {@code InvalidRiskClassifierOutputException} (the payload was
 *       rejected Ã¢â‚¬â€ retrying is wasteful and obscures the bad hash from
 *       audit).</li>
 *   <li><b>Non-retryable</b>: any successful response with
 *       {@code modelRiskLevel = 4} on the classifier pipeline Ã¢â‚¬â€ the
 *       classifier is the authoritative signal for Safety Resolver; a
 *       second call would silently change the Level 4 event timestamp.</li>
 *   <li><b>Backoff</b>: exponential doubling of
 *       {@link ProviderRetryProperties.Retry#getInitialBackoffMs()}
 *       between attempts. Bounded total wall-clock by
 *       {@code maxAttempts Ãƒâ€” requestTimeoutMs + ÃŽÂ£ backoff}.</li>
 *   <li><b>Fallback</b>: when {@link ProviderRetryProperties.Fallback#isEnabled()}
 *       is true and all retries are exhausted, the executor invokes
 *       the provided fallback callable (typically the mock provider).
 *       Fallback failure surfaces as the original exception (NOT the
 *       fallback's own exception) so the caller sees a stable failure
 *       reason.</li>
 * </ul>
 *
 * <p>Logging: the executor logs attempt counts and the final outcome
 * (success / retries-exhausted / fallback-used) at INFO/WARN levels.
 * It NEVER logs the raw input or the provider's response payload Ã¢â‚¬â€
 * those are caller responsibilities per
 * {@code .cursor/rules/30-database-ai-safety.mdc} Ã‚Â§30.
 */
public class ProviderRetryExecutor {

    private static final Logger log = LoggerFactory.getLogger(ProviderRetryExecutor.class);

    private final ProviderRetryProperties properties;

    public ProviderRetryExecutor(ProviderRetryProperties properties) {
        this.properties = properties;
    }

    /**
     * Run {@code primary} with the configured retry policy. If retries
     * are exhausted and fallback is enabled, run {@code fallback}
     * exactly once.
     *
     * @param pipelineLabel human-readable pipeline name for logging
     *                      ("chat-analysis" or "risk-classifier").
     * @param primary       the primary provider call.
     * @param fallback      the fallback provider call (only invoked if
     *                      retries exhausted AND fallback enabled).
     * @param <T>           the output type.
     * @return the result of {@code primary} or {@code fallback}.
     * @throws RuntimeException the last primary-attempt exception when
     *         fallback is disabled or also fails. The exception is one
     *         of the AI / Safety exception types declared by the
     *         provider interface.
     */
    public <T> T execute(String pipelineLabel, Callable<T> primary, Callable<T> fallback) {
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        long backoffMs = properties.getRetry().getInitialBackoffMs();

        List<RuntimeException> attempts = new ArrayList<>();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                T result = primary.call();
                if (attempt > 1) {
                    log.info("pipeline={} status=retry-success attempts={}", pipelineLabel, attempt);
                }
                return result;
            } catch (RuntimeException ex) {
                attempts.add(ex);
                if (!isRetryable(ex)) {
                    // Non-retryable Ã¢â‚¬â€ bail immediately. Audit captures
                    // the exception as-is via AiAnalysisRunService.
                    log.warn("pipeline={} status=non-retryable attempt={} exception={}",
                            pipelineLabel, attempt, ex.getClass().getSimpleName());
                    throw ex;
                }
                if (attempt >= maxAttempts) {
                    log.warn("pipeline={} status=retries-exhausted attempts={} exception={}",
                            pipelineLabel, attempt, ex.getClass().getSimpleName());
                    break;
                }
                sleep(backoffMs);
                backoffMs = Math.min(backoffMs * 2L, 5_000L); // cap doubling at 5s
                log.info("pipeline={} status=retry-scheduled attempt={} nextBackoffMs={} exception={}",
                        pipelineLabel, attempt, backoffMs, ex.getClass().getSimpleName());
            } catch (Exception ex) {
                // Any checked exception is treated as non-retryable
                // (the callables in this codebase only declare
                // RuntimeException, so this branch is defensive).
                throw new IllegalStateException(
                        "Unexpected checked exception from " + pipelineLabel + " provider", ex);
            }
        }

        // Retries exhausted. Decide on fallback.
        if (properties.getFallback().isEnabled() && fallback != null) {
            try {
                T result = fallback.call();
                log.info("pipeline={} status=fallback-used primaryException={}",
                        pipelineLabel, attempts.get(attempts.size() - 1).getClass().getSimpleName());
                return result;
            } catch (Exception fallbackEx) {
                // Fallback failure Ã¢â‚¬â€ surface the ORIGINAL primary
                // exception so the caller sees the upstream reason.
                log.warn("pipeline={} status=fallback-failed primaryException={} fallbackException={}",
                        pipelineLabel,
                        attempts.get(attempts.size() - 1).getClass().getSimpleName(),
                        fallbackEx.getClass().getSimpleName());
                throw attempts.get(attempts.size() - 1);
            }
        }

        // No fallback Ã¢â‚¬â€ surface the last primary exception.
        throw attempts.get(attempts.size() - 1);
    }

    /**
     * Whether a given exception is eligible for retry. See class JavaDoc.
     */
    public static boolean isRetryable(RuntimeException ex) {
        return ex instanceof ProviderTimeoutException
                || ex instanceof ProviderUnavailableException
                || ex instanceof RiskClassifierTimeoutException
                || ex instanceof RiskClassifierUnavailableException;
    }

    private static void sleep(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry provider call", ie);
        }
    }

    /**
     * Convenience overload for callers that have no fallback provider.
     */
    public <T> T execute(String pipelineLabel, Callable<T> primary) {
        return execute(pipelineLabel, primary, null);
    }
}
