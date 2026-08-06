package com.mindbridge.analysis.provider;

/**
 * Abstraction over any external AI/LLM provider used to analyse a single
 * conversation message. Implementations are selected at runtime via
 * {@link com.mindbridge.analysis.config.ChatAnalysisProviderConfig} based
 * on the {@code mindbridge.ai.provider} property ({@code mock} or
 * {@code real}).
 *
 * <p>Two implementations are planned for the MVP:
 * <ul>
 *   <li>{@code MockChatAnalysisProvider} — deterministic, offline, used in
 *       local and test profiles.</li>
 *   <li>{@code RealLlmChatAnalysisProvider} — calls an external LLM, gated
 *       on configuration and environment flag (G3-T06).</li>
 * </ul>
 *
 * <p>Contract (see {@code docs/01_ARCHITECTURE.md} §8):
 * <ul>
 *   <li>Implementations must validate their output before returning. JSON
 *       that does not match the expected schema must NOT be returned as a
 *       success — throw {@link com.mindbridge.analysis.exception.InvalidAnalysisOutputException}
 *       instead.</li>
 *   <li>External calls must have a timeout; retries must be limited. Both
 *       are the implementation's responsibility, not the caller's.</li>
 *   <li>Implementations must never log raw user message content. Logging
 *       is the responsibility of the consuming service.</li>
 *   <li>The {@code LLM} must not select the final CBT program — selection
 *       is a separate matching step that consumes the analysis output.</li>
 * </ul>
 */
public interface ChatAnalysisProvider {

    /**
     * Analyses a single user message and returns a structured result.
     *
     * @param input the message metadata + redacted content to analyse.
     *              Must not be {@code null}.
     * @return the structured analysis result. Never {@code null}.
     * @throws com.mindbridge.analysis.exception.ProviderTimeoutException
     *         if the underlying provider call exceeds its timeout.
     * @throws com.mindbridge.analysis.exception.ProviderUnavailableException
     *         if the provider is unreachable, returns 5xx, or rate-limits
     *         after retries are exhausted.
     * @throws com.mindbridge.analysis.exception.InvalidAnalysisOutputException
     *         if the provider returns a payload that fails schema
     *         validation.
     */
    ChatAnalysisOutput analyze(ChatAnalysisInput input);
}