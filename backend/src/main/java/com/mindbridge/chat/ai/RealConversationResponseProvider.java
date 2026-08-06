package com.mindbridge.chat.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-backed conversational response provider.
 *
 * <p>This provider is invoked only after the independent Safety pipeline has
 * resolved a message below Level 3. It never owns risk classification and it
 * never handles Level 3/4 response wording.
 */
public final class RealConversationResponseProvider implements ConversationResponseProvider {

    private static final String SYSTEM_PROMPT = """
            You are MindBridge, a supportive wellbeing conversation assistant.
            Respond in the user's language with concise, calm, non-judgmental wording.
            Do not diagnose, claim clinical certainty, or present yourself as a therapist.
            Do not create or modify CBT programs, exercises, clinical thresholds, or program states.
            Do not choose a treatment or program for the user.
            Do not generate emergency or crisis instructions; the application Safety gate handles high-risk responses before you are called.
            Acknowledge the user's message and ask at most one gentle follow-up question when useful.
            """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiBaseUrl;
    private final String apiKeyEnvVar;
    private final String model;
    private final int maxOutputTokens;
    private final String reasoningEffort;
    private final long timeoutMs;

    public RealConversationResponseProvider(
            HttpClient httpClient,
            ObjectMapper objectMapper,
            String apiBaseUrl,
            String apiKeyEnvVar,
            String model,
            int maxOutputTokens,
            String reasoningEffort,
            long timeoutMs) {
        this.httpClient = require(httpClient, "httpClient");
        this.objectMapper = require(objectMapper, "objectMapper");
        this.apiBaseUrl = requireText(apiBaseUrl, "apiBaseUrl");
        this.apiKeyEnvVar = requireText(apiKeyEnvVar, "apiKeyEnvVar");
        this.model = requireText(model, "model");
        if (maxOutputTokens <= 0) {
            throw new IllegalStateException("maxOutputTokens must be > 0");
        }
        if (timeoutMs <= 0) {
            throw new IllegalStateException("timeoutMs must be > 0");
        }
        String apiKey = System.getenv(apiKeyEnvVar);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Required AI API key environment variable is not configured");
        }
        this.maxOutputTokens = maxOutputTokens;
        this.reasoningEffort = requireText(reasoningEffort, "reasoningEffort");
        this.timeoutMs = timeoutMs;
    }

    @Override
    public String generate(ConversationResponseInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        String apiKey = System.getenv(apiKeyEnvVar);
        if (apiKey == null || apiKey.isBlank()) {
            throw new ProviderUnavailableException("AI provider credentials are unavailable");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(stripTrailingSlash(apiBaseUrl) + "/responses"))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(
                        requestBody(input), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException ex) {
            throw new ProviderTimeoutException("Conversation provider request timed out");
        } catch (IOException ex) {
            throw new ProviderUnavailableException("Conversation provider is unreachable");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProviderUnavailableException("Conversation provider request was interrupted");
        }

        int status = response.statusCode();
        if (status == 408 || status == 504 || status == 524) {
            throw new ProviderTimeoutException("Conversation provider returned a timeout");
        }
        if (status == 401 || status == 403 || status == 429 || status >= 500) {
            throw new ProviderUnavailableException(
                    "Conversation provider returned status " + status);
        }
        if (status < 200 || status >= 300) {
            throw new InvalidAnalysisOutputException(
                    "Conversation provider rejected the request with status " + status);
        }
        return extractText(response.body());
    }

    private String requestBody(ConversationResponseInput input) {
        List<Map<String, String>> messages = new ArrayList<>();
        for (ConversationResponseInput.HistoryMessage item : input.messages()) {
            messages.add(Map.of("role", item.role(), "content", item.content()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("instructions", SYSTEM_PROMPT);
        body.put("input", messages);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("reasoning", Map.of("effort", reasoningEffort));
        body.put("store", false);
        body.put("safety_identifier", safetyIdentifier(input.userId().toString()));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to serialize conversation request", ex);
        }
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode aggregated = root.path("output_text");
            if (aggregated.isTextual() && !aggregated.asText().isBlank()) {
                return validateOutputText(aggregated.asText());
            }

            JsonNode output = root.path("output");
            if (!output.isArray()) {
                throw new InvalidAnalysisOutputException(
                        "Conversation provider response has no output array");
            }

            List<String> textParts = new ArrayList<>();
            for (JsonNode item : output) {
                if (!"message".equals(item.path("type").asText())) {
                    continue;
                }
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText())
                            && part.path("text").isTextual()
                            && !part.path("text").asText().isBlank()) {
                        textParts.add(part.path("text").asText().trim());
                    }
                }
            }
            if (textParts.isEmpty()) {
                throw new InvalidAnalysisOutputException(
                        "Conversation provider response has no output text");
            }
            return validateOutputText(String.join("\n", textParts));
        } catch (IOException ex) {
            throw new InvalidAnalysisOutputException(
                    "Conversation provider returned malformed JSON");
        }
    }

    private String validateOutputText(String value) {
        String text = value.trim();
        if (text.isBlank()) {
            throw new InvalidAnalysisOutputException(
                    "Conversation provider response has no output text");
        }
        if (text.length() > 10_000) {
            throw new InvalidAnalysisOutputException(
                    "Conversation provider response exceeds the message limit");
        }
        return text;
    }

    private static String safetyIdentifier(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " must not be blank");
        }
        return value;
    }

    private static <T> T require(T value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + " must not be null");
        }
        return value;
    }
}
