package com.mindbridge.analysis.provider.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException;
import com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException;
import com.mindbridge.safety.classifier.exception.RiskClassifierUnavailableException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the G3-T07 retry + fallback policy in isolation, without
 * HTTP or DB plumbing.
 *
 * <p>DoD Â§4.3 of G3-T07: "Test Ä‘Æ°á»£c retry success, retry exhausted
 * vÃ  fallback." Plus three guards from the Phase 1 brief:
 *
 * <ul>
 *   <li>Retry-success: primary fails once then succeeds.</li>
 *   <li>Retry-exhausted: primary fails every attempt â†’ last exception
 *       propagates.</li>
 *   <li>Fallback: retries exhausted + fallback enabled â†’ fallback
 *       result wins.</li>
 *   <li>Non-retryable: {@link InvalidAnalysisOutputException} and
 *       {@link InvalidRiskClassifierOutputException} are NEVER retried.</li>
 *   <li>Timeout budget: total wall-clock is bounded by
 *       {@code maxAttempts Ã— requestTimeoutMs + Î£ backoff}.</li>
 * </ul>
 */
@DisplayName("ProviderRetryExecutor â€” retry + fallback policy")
class ProviderRetryExecutorTest {

    private ProviderRetryProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ProviderRetryProperties();
    }

    // --- Retry success ---

    @Test
    @DisplayName("Retry-success: primary fails once then succeeds on attempt 2")
    void retrySuccess_primarySucceedsOnSecondAttempt() throws Exception {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(10); // short backoff for fast test
        properties.getRetry().setRequestTimeoutMs(1000);
        properties.getFallback().setEnabled(false);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        String result = executor.execute("chat-analysis", () -> {
            int n = calls.incrementAndGet();
            if (n == 1) {
                throw new ProviderTimeoutException("first attempt timed out");
            }
            return "ok-" + n;
        });

        assertThat(result).isEqualTo("ok-2");
        assertThat(calls.get()).isEqualTo(2);
    }

    // --- Retry exhausted ---

    @Test
    @DisplayName("Retry-exhausted: primary fails every attempt â†’ last exception propagates")
    void retryExhausted_lastExceptionPropagates() {
        properties.getRetry().setMaxAttempts(3);
        properties.getRetry().setInitialBackoffMs(10);
        properties.getFallback().setEnabled(false);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("chat-analysis", () -> {
            int n = calls.incrementAndGet();
            throw new ProviderUnavailableException("attempt " + n + " unavailable");
        }))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("attempt 3");

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    @DisplayName("Retry-exhausted: max-attempts=1 â†’ only one call, exception propagates")
    void retryExhausted_defaultSingleAttempt() {
        properties.getRetry().setMaxAttempts(1);
        properties.getRetry().setInitialBackoffMs(10);
        properties.getFallback().setEnabled(false);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("chat-analysis", () -> {
            calls.incrementAndGet();
            throw new ProviderTimeoutException("nope");
        }))
                .isInstanceOf(ProviderTimeoutException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    // --- Fallback ---

    @Test
    @DisplayName("Fallback: retries exhausted + fallback enabled â†’ fallback result wins")
    void fallback_usedWhenRetriesExhaustedAndEnabled() throws Exception {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(10);
        properties.getFallback().setEnabled(true);

        AtomicInteger primaryCalls = new AtomicInteger();
        AtomicInteger fallbackCalls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        String result = executor.execute(
                "chat-analysis",
                () -> {
                    primaryCalls.incrementAndGet();
                    throw new ProviderUnavailableException("upstream broken");
                },
                () -> {
                    fallbackCalls.incrementAndGet();
                    return "from-fallback";
                });

        assertThat(result).isEqualTo("from-fallback");
        assertThat(primaryCalls.get()).isEqualTo(2);  // max-attempts=2 = 2 calls
        assertThat(fallbackCalls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fallback: disabled â†’ no fallback call, primary exception propagates")
    void fallback_disabled_doesNotInvokeFallback() {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(10);
        properties.getFallback().setEnabled(false);

        AtomicInteger fallbackCalls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute(
                "chat-analysis",
                () -> { throw new ProviderTimeoutException("first fail"); },
                () -> { fallbackCalls.incrementAndGet(); return "from-fallback"; }))
                .isInstanceOf(ProviderTimeoutException.class);

        assertThat(fallbackCalls.get()).isEqualTo(0);
    }

    @Test
    @DisplayName("Fallback: fallback itself throws â†’ original primary exception propagates")
    void fallback_failure_surfacesOriginalPrimaryException() {
        properties.getRetry().setMaxAttempts(1);
        properties.getRetry().setInitialBackoffMs(10);
        properties.getFallback().setEnabled(true);

        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute(
                "chat-analysis",
                () -> { throw new ProviderUnavailableException("upstream 502"); },
                () -> { throw new RuntimeException("fallback also broken"); }))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("upstream 502");
    }

    // --- Non-retryable ---

    @Test
    @DisplayName("Non-retryable: InvalidAnalysisOutputException is NOT retried")
    void invalidAnalysisOutput_notRetried() {
        properties.getRetry().setMaxAttempts(5);
        properties.getRetry().setInitialBackoffMs(10);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("chat-analysis", () -> {
            calls.incrementAndGet();
            throw new InvalidAnalysisOutputException("malformed payload");
        }))
                .isInstanceOf(InvalidAnalysisOutputException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Non-retryable: InvalidRiskClassifierOutputException is NOT retried")
    void invalidRiskClassifierOutput_notRetried() {
        properties.getRetry().setMaxAttempts(5);
        properties.getRetry().setInitialBackoffMs(10);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("risk-classifier", () -> {
            calls.incrementAndGet();
            throw new InvalidRiskClassifierOutputException("malformed classifier payload");
        }))
                .isInstanceOf(InvalidRiskClassifierOutputException.class);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Retryable: RiskClassifierTimeoutException IS retried")
    void riskClassifierTimeout_isRetried() {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(10);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("risk-classifier", () -> {
            calls.incrementAndGet();
            throw new RiskClassifierTimeoutException("classifier slow");
        }))
                .isInstanceOf(RiskClassifierTimeoutException.class);

        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("Retryable: RiskClassifierUnavailableException IS retried")
    void riskClassifierUnavailable_isRetried() {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(10);

        AtomicInteger calls = new AtomicInteger();
        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);

        assertThatThrownBy(() -> executor.execute("risk-classifier", () -> {
            calls.incrementAndGet();
            throw new RiskClassifierUnavailableException("classifier 503");
        }))
                .isInstanceOf(RiskClassifierUnavailableException.class);

        assertThat(calls.get()).isEqualTo(2);
    }

    // --- Static helper ---

    @Test
    @DisplayName("isRetryable: only the four exception types are retryable")
    void isRetryable_classification() {
        assertThat(ProviderRetryExecutor.isRetryable(new ProviderTimeoutException())).isTrue();
        assertThat(ProviderRetryExecutor.isRetryable(new ProviderUnavailableException())).isTrue();
        assertThat(ProviderRetryExecutor.isRetryable(new RiskClassifierTimeoutException())).isTrue();
        assertThat(ProviderRetryExecutor.isRetryable(new RiskClassifierUnavailableException())).isTrue();
        assertThat(ProviderRetryExecutor.isRetryable(new InvalidAnalysisOutputException())).isFalse();
        assertThat(ProviderRetryExecutor.isRetryable(new InvalidRiskClassifierOutputException())).isFalse();
        assertThat(ProviderRetryExecutor.isRetryable(new IllegalStateException("other"))).isFalse();
    }

    // --- Timeout budget ---

    @Test
    @DisplayName("Timeout budget: total wall-clock is bounded by config (does not hang)")
    void timeoutBudget_isBounded() {
        properties.getRetry().setMaxAttempts(2);
        properties.getRetry().setInitialBackoffMs(50);  // 50ms first backoff
        properties.getRetry().setRequestTimeoutMs(100);
        properties.getFallback().setEnabled(false);

        ProviderRetryExecutor executor = new ProviderRetryExecutor(properties);
        long start = System.currentTimeMillis();
        assertThatThrownBy(() -> executor.execute("chat-analysis", () -> {
            throw new ProviderTimeoutException("simulated timeout");
        }))
                .isInstanceOf(ProviderTimeoutException.class);
        long elapsed = System.currentTimeMillis() - start;

        // Generous upper bound â€” the only requirement is "not unbounded".
        // With maxAttempts=2 and initialBackoffMs=50, total sleep is at
        // most ~50ms (between attempts). We allow 2s slack for JVM warmup.
        assertThat(elapsed).as("must finish promptly (no hang)").isLessThan(2000L);
    }
}
