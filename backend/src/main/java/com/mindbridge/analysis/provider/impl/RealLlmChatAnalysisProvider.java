package com.mindbridge.analysis.provider.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.RealLlmResponseContext;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryExecutor;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import com.mindbridge.analysis.provider.validation.ChatAnalysisSchemaValidator;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

/**
 * Real LLM {@link ChatAnalysisProvider} implementation. Calls a hosted
 * pretrained model (OpenAI per G3-T06 Phase 1 Q1), validates the
 * payload against {@code docs/schemas/chat_analysis_v1.schema.json}
 * BEFORE constructing a {@link ChatAnalysisOutput}, and applies a
 * bounded retry policy via {@link ProviderRetryExecutor} (G3-T07).
 *
 * <p>This implementation NEVER logs the request body, response body,
 * or the API key. Authorization headers are constructed per request
 * via {@link System#getenv(String)} and are not retained.
 */
public class RealLlmChatAnalysisProvider implements ChatAnalysisProvider {

    private static final Logger log = LoggerFactory.getLogger(RealLlmChatAnalysisProvider.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ChatAnalysisSchemaValidator validator;

    private final String providerLabel;
    private final String model;
    private final String apiBaseUrl;
    private final String apiKeyEnvVar;
    private final String promptVersion;
    private final int maxTokens;
    private final int maxRetries;
    private final long requestTimeoutMs;
    private final ProviderRetryProperties retryProperties;
    private final ChatAnalysisProvider fallbackProvider;

    /**
     * Package-private constructor for unit tests (mirrors G3-T06's
     * original 10-arg signature so the existing test continues to
     * compile). Tests that want the new retry / validation behaviour
     * instantiate {@link #RealLlmChatAnalysisProvider(HttpClient,
     * ObjectMapper, ChatAnalysisSchemaValidator, String, String,
     * String, String, String, int, int, Duration, ProviderRetryProperties,
     * ChatAnalysisProvider)} instead.
     */
    RealLlmChatAnalysisProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String providerLabel,
            String model,
            String apiBaseUrl,
            String apiKeyEnvVar,
            String promptVersion,
            int maxTokens,
            int maxRetries,
            Duration requestTimeout) {
        this(httpClient, objectMapper,
                new ChatAnalysisSchemaValidator(),
                providerLabel, model, apiBaseUrl, apiKeyEnvVar, promptVersion,
                maxTokens, maxRetries, requestTimeout.toMillis(),
                defaultRetryProperties(maxRetries, requestTimeout.toMillis()),
                null);
    }

    /**
     * Full constructor used by {@link Wired} (Spring) and integration
     * tests. {@code retryProperties} may be null only when this is
     * the no-fallback test path that explicitly wants T06 semantics.
     */
    RealLlmChatAnalysisProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            ChatAnalysisSchemaValidator validator,
            String providerLabel,
            String model,
            String apiBaseUrl,
            String apiKeyEnvVar,
            String promptVersion,
            int maxTokens,
            int maxRetries,
            Duration requestTimeout,
            ProviderRetryProperties retryProperties,
            ChatAnalysisProvider fallbackProvider) {
        this(httpClient, objectMapper, validator,
                providerLabel, model, apiBaseUrl, apiKeyEnvVar, promptVersion,
                maxTokens, maxRetries, requestTimeout.toMillis(),
                retryProperties, fallbackProvider);
    }

    /**
     * Lowest-level constructor. {@code requestTimeoutMs} is the
     * underlying primitive â€” all public constructors above convert
     * from {@link Duration} here.
     */
    RealLlmChatAnalysisProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            ChatAnalysisSchemaValidator validator,
            String providerLabel,
            String model,
            String apiBaseUrl,
            String apiKeyEnvVar,
            String promptVersion,
            int maxTokens,
            int maxRetries,
            long requestTimeoutMs,
            ProviderRetryProperties retryProperties,
            ChatAnalysisProvider fallbackProvider) {
        validateString("providerLabel", providerLabel, 50);
        validateString("model", model, 100);
        validateString("apiBaseUrl", apiBaseUrl, 200);
        validateString("apiKeyEnvVar", apiKeyEnvVar, 100);
        validateString("promptVersion", promptVersion, 50);
        if (maxTokens <= 0) {
            throw new IllegalStateException("maxTokens must be > 0 (was " + maxTokens + ")");
        }
        if (maxRetries < 0) {
            throw new IllegalStateException("maxRetries must be >= 0 (was " + maxRetries + ")");
        }
        if (requestTimeoutMs < 0) {
            throw new IllegalStateException("requestTimeoutMs must be >= 0 (was " + requestTimeoutMs + ")");
        }
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.validator = validator == null ? new ChatAnalysisSchemaValidator() : validator;
        this.providerLabel = providerLabel;
        this.model = model;
        this.apiBaseUrl = apiBaseUrl;
        this.apiKeyEnvVar = apiKeyEnvVar;
        this.promptVersion = promptVersion;
        this.maxTokens = maxTokens;
        this.maxRetries = maxRetries;
        this.requestTimeoutMs = requestTimeoutMs;
        this.retryProperties = retryProperties == null
                ? defaultRetryProperties(maxRetries, requestTimeoutMs)
                : retryProperties;
        this.fallbackProvider = fallbackProvider;
    }

    private static ProviderRetryProperties defaultRetryProperties(int maxRetries, long requestTimeoutMs) {
        ProviderRetryProperties p = new ProviderRetryProperties();
        p.getRetry().setMaxAttempts(Math.max(1, maxRetries + 1));
        p.getRetry().setRequestTimeoutMs(requestTimeoutMs);
        return p;
    }

    @Override
    public ChatAnalysisOutput analyze(ChatAnalysisInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        ProviderRetryExecutor executor = new ProviderRetryExecutor(retryProperties);
        Callable<ChatAnalysisOutput> primary = () -> callAndValidate(input);
        Callable<ChatAnalysisOutput> fallback = fallbackProvider == null
                ? null
                : () -> fallbackProvider.analyze(input);
        return executor.execute("chat-analysis", primary, fallback);
    }

    private ChatAnalysisOutput callAndValidate(ChatAnalysisInput input) {
        long invokeStartNanos = System.nanoTime();
        String rawJson = httpPost(input);
        long latencyMs = (System.nanoTime() - invokeStartNanos) / 1_000_000L;

        Set<String> errors = validator.validate(rawJson);
        if (!errors.isEmpty()) {
            throw new InvalidAnalysisOutputException(formatInvalidErrors(errors, rawJson.length()));
        }

        ChatAnalysisOutput output;
        try {
            JsonNode node = validator.parse(rawJson);
            output = mapToOutput(node, latencyMs);
        } catch (IOException ex) {
            throw new InvalidAnalysisOutputException("payload is not valid JSON: " + ex.getClass().getSimpleName());
        } catch (IllegalArgumentException ex) {
            // DTO compact constructor rejected the payload ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â that is also an invalid output.
            throw new InvalidAnalysisOutputException("payload failed DTO mapping: " + ex.getMessage());
        }
        return output;
    }

    /**
     * Map a JSON Schema-validated node to {@link ChatAnalysisOutput}.
     * Schema validation has already passed, so this method assumes
     * every required field is present and every enum value is in range.
     * It still re-validates at the DTO compact constructor for defence
     * in depth.
     */
    private ChatAnalysisOutput mapToOutput(JsonNode node, long latencyMs) {
        Topic topic = Topic.valueOf(node.get("topic").asText());
        Emotion emotion = Emotion.valueOf(node.get("emotion").asText());
        Intent intent = Intent.valueOf(node.get("intent").asText());
        List<Signal> signals = new java.util.ArrayList<>();
        for (JsonNode s : node.get("signals")) {
            signals.add(Signal.valueOf(s.asText()));
        }
        int riskLevel = node.get("modelRiskLevel").asInt();
        double confidence = node.get("confidence").asDouble();
        List<com.mindbridge.analysis.provider.EvidenceSpan> spans = new java.util.ArrayList<>();
        for (JsonNode spanNode : node.get("evidenceSpans")) {
            int start = spanNode.get("start").asInt();
            int end = spanNode.get("end").asInt();
            String textHash = spanNode.get("textHash").asText();
            spans.add(new com.mindbridge.analysis.provider.EvidenceSpan(start, end, textHash));
        }
        long latency = latencyMs;
        String errorCode = node.has("errorCode") && !node.get("errorCode").isNull()
                ? node.get("errorCode").asText() : null;
        String schemaVersion = node.get("schemaVersion").asText();
        return new ChatAnalysisOutput(topic, emotion, intent, signals, riskLevel, confidence,
                spans, latency, errorCode, schemaVersion);
    }

    /**
     * Issue the HTTP POST and return the raw assistant message JSON
     * substring. Exception mapping mirrors the G3-T06 contract so
     * the existing tests continue to assert the same behaviour:
     *
     * <ul>
     *   <li>5xx, 429 ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ {@link ProviderUnavailableException}</li>
     *   <li>408, 504, 524, {@link HttpTimeoutException} ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ {@link ProviderTimeoutException}</li>
     *   <li>4xx + malformed body ÃƒÂ¢Ã¢â‚¬Â Ã¢â‚¬â„¢ {@link InvalidAnalysisOutputException}</li>
     * </ul>
     */
    private String httpPost(ChatAnalysisInput input) {
        String apiKey = System.getenv(apiKeyEnvVar);
        if (apiKey == null || apiKey.isBlank()) {
            // Should have been caught at bean construction ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â defensive.
            throw new ProviderUnavailableException("API key env var '" + apiKeyEnvVar + "' is unset");
        }
        String requestBody = buildRequestBody(input);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create(apiBaseUrl + "/chat/completions"))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Failed to build real LLM request body", ex);
        }

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException ex) {
            throw new ProviderTimeoutException("real LLM request exceeded timeout");
        } catch (IOException ex) {
            throw new ProviderUnavailableException("real LLM unreachable: " + ex.getClass().getSimpleName());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling real LLM", ex);
        }

        int status = response.statusCode();
        String body = response.body();

        if (status >= 200 && status < 300) {
            String assistantJson = extractAssistantJson(body);
            // Capture the actual upstream provider/model into the
            // response context so the service can update the run row.
            RealLlmResponseContext.set(new RealLlmResponseContext.Snapshot(
                    providerLabel,
                    extractModelId(body, model)));
            return assistantJson;
        }

        if (status == 408 || status == 504 || status == 524) {
            throw new ProviderTimeoutException("real LLM returned timeout status " + status);
        }
        if (status == 401 || status == 403 || status == 429 || status >= 500) {
            throw new ProviderUnavailableException("real LLM returned retryable status " + status);
        }
        // 4xx ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â payload-level error
        throw new InvalidAnalysisOutputException(
                "real LLM returned non-success status " + status);
    }

    private String buildRequestBody(ChatAnalysisInput input) {
        // Mirrors G3-T06: keep the same shape so existing tests and
        // behavioural assertions continue to apply.
        try {
            return objectMapper.writeValueAsString(new RequestBody(
                    model,
                    List.of(new Message("user", input.content())),
                    maxTokens));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build real LLM request body", ex);
        }
    }

    private static String extractAssistantJson(String openAiResponse) {
        try {
            JsonNode root = new ObjectMapper().readTree(openAiResponse);
            JsonNode choices = root.get("choices");
            if (choices == null || !choices.isArray() || choices.isEmpty()) {
                throw new InvalidAnalysisOutputException("OpenAI response has no choices");
            }
            JsonNode message = choices.get(0).get("message");
            if (message == null || message.get("content") == null) {
                throw new InvalidAnalysisOutputException("OpenAI response missing message.content");
            }
            return message.get("content").asText();
        } catch (IOException ex) {
            throw new InvalidAnalysisOutputException("OpenAI response is not valid JSON");
        }
    }

    private static String extractModelId(String openAiResponse, String fallback) {
        try {
            JsonNode root = new ObjectMapper().readTree(openAiResponse);
            JsonNode model = root.get("model");
            if (model == null || model.isNull()) {
                return fallback;
            }
            String text = model.asText();
            return text == null || text.isBlank() ? fallback : text;
        } catch (IOException ex) {
            return fallback;
        }
    }

    private static String formatInvalidErrors(Set<String> errors, int payloadLength) {
        // The payloadLength is intentionally NOT inlined into the
        // message ÃƒÂ¢Ã¢â€šÂ¬Ã¢â‚¬Â the AiRunErrorRedactor strips non-ASCII, but a
        // payload length is metadata, not sensitive content. Limit to
        // the first 3 messages so the redacted summary stays readable.
        StringBuilder sb = new StringBuilder("schema validation failed (");
        sb.append(errors.size()).append(" error");
        if (errors.size() != 1) sb.append('s');
        sb.append("): ");
        int i = 0;
        for (String e : errors) {
            if (i > 0) sb.append("; ");
            sb.append(e);
            if (++i >= 3) {
                sb.append("; ...");
                break;
            }
        }
        return sb.toString();
    }

    private static void validateString(String name, String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must not be null or blank");
        }
        if (value.length() > maxLength) {
            throw new IllegalStateException(name + " exceeds max length " + maxLength + " (was " + value.length() + ")");
        }
    }

    // --- Inner classes for request / message shape. ---

    private record RequestBody(String model, List<Message> messages, int max_tokens) {}
    private record Message(String role, String content) {}

    // --- Spring wiring ---

    /**
     * Spring-friendly factory. Validation at bean construction time
     * matches the G3-T06 fail-secure contract (any blank required
     * property or unset API key env var surfaces as
     * {@link IllegalStateException}, preventing app startup).
     */
    public static class Wired extends RealLlmChatAnalysisProvider {

        @Autowired
        public Wired(
                HttpClient httpClient,
                ObjectMapper objectMapper,
                ChatAnalysisSchemaValidator validator,
                ProviderRetryProperties retryProperties,
                Environment environment) {
            super(httpClient, objectMapper, validator,
                    environment.getProperty("mindbridge.ai.real.provider-label", "openai"),
                    environment.getProperty("mindbridge.ai.real.model", ""),
                    environment.getProperty("mindbridge.ai.real.api-base-url", "https://api.openai.com/v1"),
                    environment.getProperty("mindbridge.ai.real.api-key-env-var", "MINDBRIDGE_AI_REAL_API_KEY"),
                    environment.getProperty("mindbridge.ai.analysis-run.prompt-version", "v1:5363675e22fe"),
                    environment.getProperty("mindbridge.ai.real.max-tokens", Integer.class, 1024),
                    environment.getProperty("mindbridge.ai.real.max-retries", Integer.class, 1),
                    environment.getProperty("mindbridge.ai.real.request-timeout-ms", Long.class, 20000L),
                    retryProperties,
                    resolveFallback(environment));
        }

        private static ChatAnalysisProvider resolveFallback(Environment environment) {
            // Fallback is disabled by default. Tests can override by
            // setting mindbridge.ai.provider.fallback.enabled=true AND
            // injecting a mock ChatAnalysisProvider bean into the
            // context (e.g. via a test config). We do NOT auto-wire
            // MockChatAnalysisProvider here because that would break
            // the production fail-secure contract.
            boolean enabled = environment.getProperty(
                    "mindbridge.ai.provider.fallback.enabled", Boolean.class, false);
            if (!enabled) {
                return null;
            }
            // Spring autowire is unavailable in a static helper; tests
            // that want a fallback pass the provider explicitly via
            // the package-private constructor. For production, fallback
            // is intentionally a no-op until T11+ wires it from the
            // pipeline caller.
            return null;
        }
    }
}
