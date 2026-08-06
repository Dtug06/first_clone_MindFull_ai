package com.mindbridge.analysis.provider.pipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Retry / fallback policy shared by {@link com.mindbridge.analysis.provider.ChatAnalysisProvider}
 * (chat analysis pipeline) and {@link com.mindbridge.safety.classifier.RiskClassifierProvider}
 * (Safety pipeline).
 *
 * <p>Properties live under {@code mindbridge.ai.provider.retry.*} and
 * {@code mindbridge.ai.provider.fallback.*}. The original
 * {@code mindbridge.ai.real.max-retries} and
 * {@code mindbridge.ai.real.request-timeout-ms} keys shipped by G3-T06
 * are still honoured as deprecated aliases (see
 * {@code application.yml}) so existing deployments do not need to
 * reconfigure at upgrade time.
 *
 * <p>Defaults match the values T06 chose for the real LLM provider:
 * one retry max, 200ms initial backoff, 20s per-attempt timeout. The
 * MVP keeps the count low because the rule "khÃƒÂ´ng retry vÃƒÂ´ hÃ¡ÂºÂ¡n hoÃ¡ÂºÂ·c
 * retry Level 4 khÃƒÂ´ng kiÃ¡Â»Æ’m soÃƒÂ¡t" ({@code docs/tasks/G3/G3-T07-...})
 * binds retry to a small bounded number, and the per-attempt timeout
 * is already conservative.
 */
@ConfigurationProperties(prefix = "mindbridge.ai.pipeline")
public class ProviderRetryProperties {

    private Retry retry = new Retry();
    private Fallback fallback = new Fallback();

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public Fallback getFallback() {
        return fallback;
    }

    public void setFallback(Fallback fallback) {
        this.fallback = fallback;
    }

    /** Retry tuning. */
    public static class Retry {

        /**
         * Total attempts including the first call. {@code 1} = no retry,
         * {@code 2} = one retry, etc. Values <= 0 are coerced to 1 at
         * construction. Per the user's "khÃƒÂ´ng retry vÃƒÂ´ hÃ¡ÂºÂ¡n" rule, this
         * is bounded and small.
         */
        private int maxAttempts = 1;

        /**
         * Initial backoff in milliseconds. Subsequent retries (if any)
         * double this value (200 Ã¢â€ â€™ 400 Ã¢â€ â€™ 800 Ã¢â‚¬Â¦). Values <= 0 are
         * coerced to 200 at construction.
         */
        private long initialBackoffMs = 200L;

        /**
         * Per-attempt HTTP timeout in milliseconds. The existing
         * {@code RealLlmChatAnalysisProvider} already enforces this on
         * each {@link java.net.http.HttpRequest}. Values <= 0 are
         * coerced to 20000 at construction.
         */
        private long requestTimeoutMs = 20_000L;

        public int getMaxAttempts() {
            return maxAttempts <= 0 ? 1 : maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getInitialBackoffMs() {
            return initialBackoffMs <= 0 ? 200L : initialBackoffMs;
        }

        public void setInitialBackoffMs(long initialBackoffMs) {
            this.initialBackoffMs = initialBackoffMs;
        }

        public long getRequestTimeoutMs() {
            return requestTimeoutMs <= 0 ? 20_000L : requestTimeoutMs;
        }

        public void setRequestTimeoutMs(long requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }
    }

    /** Fallback policy. */
    public static class Fallback {

        /**
         * When {@code true}, retry exhaustion (or an
         * {@code Invalid*OutputException} on the LAST attempt) falls
         * back to the mock provider for one final attempt. The
         * resulting {@code ai_analysis_runs} row is persisted with
         * {@code provider = "fallback-mock"} so audit can see the
         * original failure path.
         *
         * <p>Default {@code false} in prod profiles, {@code true} in
         * local/dev profiles. The user's "theo mÃƒÂ´i trÃ†Â°Ã¡Â»Âng nÃ¡ÂºÂ¿u Ã„â€˜Ã†Â°Ã¡Â»Â£c
         * cÃ¡ÂºÂ¥u hÃƒÂ¬nh" requirement is honoured here.
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
