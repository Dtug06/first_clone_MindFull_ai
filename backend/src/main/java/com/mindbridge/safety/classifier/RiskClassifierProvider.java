package com.mindbridge.safety.classifier;

/**
 * Abstraction over any external AI/LLM provider used to classify a
 * single user message into a Safety risk signal. Implementations are
 * selected at runtime via
 * {@link com.mindbridge.safety.classifier.config.RiskClassifierProviderConfig}
 * based on the {@code mindbridge.ai.risk-classifier.provider} property.
 *
 * <p><b>Why a separate interface from
 * {@code com.mindbridge.analysis.provider.ChatAnalysisProvider}:</b>
 * per {@code docs/tasks/G3/G3-T09-llm-risk-classification-rieng.md}
 * ("Tách risk classifier khỏi câu trả lời chatbot") and
 * {@code docs/01_ARCHITECTURE.md} §9, the LLM risk signal is its own
 * pipeline layer. It has a different output schema ({@code reasonCodes}
 * vs chat analysis {@code signals}), a different caller (Safety
 * Resolver G3-T10 vs chat consumer), and independent failure isolation
 * (a classifier outage does not break chat analysis).
 *
 * <p>Two implementations are planned:
 * <ul>
 *   <li>{@code MockRiskClassifierProvider} — deterministic, offline,
 *       used in local and test profiles.</li>
 *   <li>{@code RealLlmRiskClassifierProvider} — calls an external LLM,
 *       gated on configuration and environment flag (post-MVP; T09
 *       ships mock only).</li>
 * </ul>
 *
 * <p>Contract:
 * <ul>
 *   <li>Implementations must validate their output before returning.
 *       Any payload that fails {@link RiskClassifierOutput}'s compact
 *       constructor must NOT be returned as a success — throw
 *       {@link com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException}
 *       instead.</li>
 *   <li>External calls must have a timeout; retries must be limited.
 *       Both are the implementation's responsibility, not the caller's.</li>
 *   <li>Implementations must never log raw user message content. This
 *       is the responsibility of the consuming service per
 *       {@code .cursor/rules/30-database-ai-safety.mdc} §AI Rules
 *       ("Do not log unnecessary raw prompts or raw responses
 *       containing sensitive data").</li>
 *   <li>This provider returns {@code riskLevel} only — it does NOT
 *       decide {@code final_risk_level}. The Safety Resolver combines
 *       this signal with the keyword/regex pre-filter
 *       (G3-T08 {@code SafetyPreFilterService}) and the user's current
 *       risk state to compute the final risk. See
 *       {@code docs/04_SAFETY_AND_CBT_RULES.md} §3.2 ("Không được nâng
 *       hoặc hạ risk chỉ dựa trên một từ khóa đơn lẻ") and §5
 *       ("Phải phân biệt model_risk_level / rule_risk_level /
 *       final_risk_level").</li>
 * </ul>
 */
public interface RiskClassifierProvider {

    /**
     * Classifies a single user message and returns a structured Safety
     * signal.
     *
     * @param input the message metadata + redacted content to classify.
     *              Must not be {@code null}.
     * @return the structured risk signal. Never {@code null}.
     * @throws com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException
     *         if the underlying provider call exceeds its timeout.
     * @throws com.mindbridge.safety.classifier.exception.RiskClassifierUnavailableException
     *         if the provider is unreachable, returns 5xx, or rate-limits
     *         after retries are exhausted.
     * @throws com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException
     *         if the provider returns a payload that fails schema
     *         validation.
     */
    RiskClassifierOutput classify(RiskClassifierInput input);
}
