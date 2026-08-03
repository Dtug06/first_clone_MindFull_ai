package com.mindbridge.analysis.provider.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.RealLlmResponseContext;
import com.mindbridge.analysis.provider.pipeline.ProviderRetryProperties;
import com.mindbridge.analysis.provider.validation.ChatAnalysisSchemaValidator;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * Integration test for G3-T07 - exercises the full
 * {@link RealLlmChatAnalysisProvider} pipeline through the new
 * {@link ChatAnalysisSchemaValidator} + {@link ProviderRetryProperties}
 * + retry executor.
 *
 * <p>Direct coverage of G3-T07 DoD 4.1 + 4.3:
 *
 * <ul>
 *   <li>Non-200 response -> retryable exception bubbles up (via retry executor).</li>
 *   <li>200 response with malformed payload -> validator returns
 *       errors -> {@link InvalidAnalysisOutputException} (no fallback
 *       attempted because Invalid* is non-retryable).</li>
 *   <li>Retries exhausted + fallback enabled -> fallback wins.</li>
 *   <li>Retries exhausted + fallback disabled -> last exception
 *       propagates.</li>
 * </ul>
 */
@DisplayName("RealLlmChatAnalysisProvider - G3-T07 end-to-end pipeline")
class RealLlmChatAnalysisProviderG3T07IntegrationTest {

    private static final String ENV_VAR = "MINDBRIDGE_AI_REAL_API_KEY";

    private ObjectMapper objectMapper;
    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        httpClient = mock(HttpClient.class);
    }

    @AfterEach
    void tearDown() {
        RealLlmResponseContext.clear();
    }

    private ChatAnalysisInput sampleInput() {
        return new ChatAnalysisInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Toi cam thay rat met moi. Cong viec qua ap luc.",
                "vi-VN");
    }

    /** Sample successful OpenAI response wrapping a valid L2 payload. */
    private String successL2Response() {
        return """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "model": "gpt-4o-mini-2025-01-01",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\"topic\\":\\"WORK_STRESS\\",\\"emotion\\":\\"ANXIOUS\\",\\"intent\\":\\"VENT\\",\\"signals\\":[\\"BURNOUT\\"],\\"modelRiskLevel\\":2,\\"confidence\\":0.78,\\"evidenceSpans\\":[],\\\"latencyMs\\":25,\\\"errorCode\\\":null,\\\"schemaVersion\\":\\\"V1\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;
    }

    /** Sample OpenAI response wrapping a malformed payload (missing topic). */
    private String malformedPayloadResponse() {
        return """
                {
                  "id": "chatcmpl-test",
                  "model": "gpt-4o-mini-2025-01-01",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "{\\\"emotion\\\":\\\"NEUTRAL\\\",\\\"intent\\\":\\\"VENT\\\",\\\"signals\\\":[],\\\"modelRiskLevel\\\":1,\\\"confidence\\\":0.5,\\\"evidenceSpans\\\":[],\\\"latencyMs\\\":0,\\\"errorCode\\\":null,\\\"schemaVersion\\\":\\\"V1\\\"}"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
                """;
    }

    /** Build a stubbed HttpResponse with the given status + body. */
    @SuppressWarnings("unchecked")
    private HttpResponse<String> stubResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    /** JSON sent back from the mock fallback. */
    private ChatAnalysisOutput fallbackOutput() {
        return new ChatAnalysisOutput(
                com.mindbridge.analysis.provider.Topic.WORK_STRESS,
                com.mindbridge.analysis.provider.Emotion.NEUTRAL,
                com.mindbridge.analysis.provider.Intent.VENT,
                java.util.List.of(),
                1,
                0.5,
                java.util.List.of(),
                0L,
                null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
    }

    // --- Successful path (validation passes) ---

    @Test
    @DisplayName("Successful path: real provider returns valid L2 payload -> maps to ChatAnalysisOutput")
    void validL2Payload_passes() throws Exception {
        HttpResponse<String> stub = stubResponse(200, successL2Response());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) stub);
        ChatAnalysisProvider provider = buildProvider(new ProviderRetryProperties(), null);

        ChatAnalysisOutput result = provider.analyze(sampleInput());
        assertThat(result.topic()).isEqualTo(com.mindbridge.analysis.provider.Topic.WORK_STRESS);
        assertThat(result.modelRiskLevel()).isEqualTo(2);
        assertThat(result.confidence()).isEqualTo(0.78);
    }

    // --- Malformed payload -> InvalidAnalysisOutputException (NOT retried) ---

    @Test
    @DisplayName("Malformed payload: schema validation fails -> InvalidAnalysisOutputException, no retry")
    void malformedPayload_notRetried() throws Exception {
        // Every call returns the same malformed payload. If the
        // provider were to retry, we'd see > 1 send() call. We
        // configure an Answer that increments a counter so the
        // assertion below is robust against any accidental retry.
        final int[] callCount = {0};
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(new Answer<HttpResponse<String>>() {
            @Override
            public HttpResponse<String> answer(InvocationOnMock invocation) {
                callCount[0]++;
                return stubResponse(200, malformedPayloadResponse());
            }
        });

        ProviderRetryProperties props = new ProviderRetryProperties();
        props.getRetry().setMaxAttempts(5);  // would retry 5 times if retryable
        props.getRetry().setInitialBackoffMs(10);

        ChatAnalysisProvider provider = buildProvider(props, null);

        assertThatThrownBy(() -> provider.analyze(sampleInput()))
                .isInstanceOf(InvalidAnalysisOutputException.class)
                .hasMessageContaining("schema validation failed");

        // Critical: only ONE HTTP call. The validator catches bad
        // payloads BEFORE the retry executor schedules another attempt.
        assertThat(callCount[0])
                .as("InvalidAnalysisOutputException must short-circuit retry")
                .isEqualTo(1);
    }

    // --- Retry -> exhaustion -> fallback (enabled) ---

    @Test
    @DisplayName("Retry exhausted + fallback enabled -> fallback wins")
    void retryExhausted_fallbackWins() throws Exception {
        HttpResponse<String> stub = stubResponse(503, "{\"error\":\"upstream broken\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) stub);

        ProviderRetryProperties props = new ProviderRetryProperties();
        props.getRetry().setMaxAttempts(3);
        props.getRetry().setInitialBackoffMs(10);
        props.getFallback().setEnabled(true);

        ChatAnalysisProvider fallback = new ChatAnalysisProvider() {
            @Override
            public ChatAnalysisOutput analyze(ChatAnalysisInput input) {
                return fallbackOutput();
            }
        };

        ChatAnalysisProvider provider = buildProvider(props, fallback);

        ChatAnalysisOutput result = provider.analyze(sampleInput());
        // The fallback output is what wins:
        assertThat(result.modelRiskLevel()).isEqualTo(1);
        assertThat(result.confidence()).isEqualTo(0.5);
        assertThat(result.signals()).isEmpty();
    }

    // --- Retry exhausted -> no fallback -> exception propagates ---

    @Test
    @DisplayName("Retry exhausted + fallback disabled -> ProviderUnavailableException propagates")
    void retryExhausted_noFallback_exceptionPropagates() throws Exception {
        HttpResponse<String> stub = stubResponse(503, "{\"error\":\"upstream broken\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) stub);

        ProviderRetryProperties props = new ProviderRetryProperties();
        props.getRetry().setMaxAttempts(2);
        props.getRetry().setInitialBackoffMs(10);
        props.getFallback().setEnabled(false);

        ChatAnalysisProvider provider = buildProvider(props, null);

        assertThatThrownBy(() -> provider.analyze(sampleInput()))
                .isInstanceOf(ProviderUnavailableException.class);
    }

    // --- Successful path uses RealLlmResponseContext ---

    @Test
    @DisplayName("Successful path: RealLlmResponseContext is set with actual upstream model")
    void validPayload_setsRealLlmResponseContext() throws Exception {
        HttpResponse<String> stub = stubResponse(200, successL2Response());
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((HttpResponse) stub);
        ChatAnalysisProvider provider = buildProvider(new ProviderRetryProperties(), null);

        RealLlmResponseContext.clear();
        provider.analyze(sampleInput());

        RealLlmResponseContext.Snapshot snapshot = RealLlmResponseContext.current();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.provider()).isEqualTo("openai");
        assertThat(snapshot.model()).isEqualTo("gpt-4o-mini-2025-01-01");
    }

    // --- Helpers ---

    private RealLlmChatAnalysisProvider buildProvider(
            ProviderRetryProperties props, ChatAnalysisProvider fallback) {
        return new RealLlmChatAnalysisProvider(
                httpClient,
                objectMapper,
                new ChatAnalysisSchemaValidator(),
                "openai",
                "gpt-4o-mini",
                "https://api.example.test/v1",
                ENV_VAR,
                "v1:5363675e22fe",
                1024,
                1,   // maxRetries (legacy)
                Duration.ofSeconds(1),
                props,
                fallback);
    }
}
