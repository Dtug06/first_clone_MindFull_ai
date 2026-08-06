package com.mindbridge.chat.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mindbridge.chat.personalization.PersonalizationContext;
import java.math.BigDecimal;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RealConversationResponseProviderTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void sendsSafeResponsesApiRequestAndExtractsAssistantText() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", exchange -> {
            requestBody.set(readBody(exchange));
            respond(exchange, 200,
                    "{\"model\":\"gpt-5.5\",\"output\":["
                            + "{\"type\":\"reasoning\",\"summary\":[]},"
                            + "{\"type\":\"message\",\"role\":\"assistant\",\"content\":["
                            + "{\"type\":\"output_text\",\"text\":\"A calm reply\"}]}]}");
        });

        RealConversationResponseProvider provider = new RealConversationResponseProvider(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                baseUrl,
                "MINDBRIDGE_AI_REAL_API_KEY",
                "gpt-5.5",
                300,
                "low",
                5_000);

        String response = provider.generate(new ConversationResponseInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new ConversationResponseInput.HistoryMessage(
                        "user", "I need a quiet moment"))));

        assertThat(response).isEqualTo("A calm reply");
        JsonNode sent = parse(requestBody.get());
        assertThat(sent.path("model").asText()).isEqualTo("gpt-5.5");
        assertThat(sent.path("store").asBoolean()).isFalse();
        assertThat(sent.path("safety_identifier").asText()).hasSize(64);
        assertThat(sent.path("instructions").asText()).contains("Safety gate");
        assertThat(sent.path("input").get(0).path("role").asText())
                .isEqualTo("user");
        assertThat(sent.path("max_output_tokens").asInt()).isEqualTo(300);
        assertThat(sent.path("reasoning").path("effort").asText()).isEqualTo("low");
    }

    @Test
    void sendsOnlyBoundedPersonalizationContextWhenAvailable() {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server.createContext("/responses", exchange -> {
            requestBody.set(readBody(exchange));
            respond(exchange, 200, "{\"output_text\":\"Chao Minh\"}");
        });

        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        RealConversationResponseProvider provider = new RealConversationResponseProvider(
                HttpClient.newHttpClient(), mapper, baseUrl,
                "MINDBRIDGE_AI_REAL_API_KEY", "gpt-5.5", 300, "low", 5_000);
        PersonalizationContext context = new PersonalizationContext(
                "Minh", LocalDate.of(2026, 8, 6),
                List.of(new PersonalizationContext.DailyObservation(
                        "STRESS", new BigDecimal("4"), null)),
                null);

        provider.generate(new ConversationResponseInput(
                UUID.randomUUID(), UUID.randomUUID(),
                List.of(new ConversationResponseInput.HistoryMessage("user", "Ban nho ten toi khong?")),
                context));

        String instructions = parse(requestBody.get()).path("instructions").asText();
        assertThat(instructions)
                .contains("PERSONALIZATION_CONTEXT", "\"displayName\":\"Minh\"", "\"code\":\"STRESS\"")
                .doesNotContain("textValue", "email", "password");
    }

    private JsonNode parse(String value) {
        try {
            return new ObjectMapper().readTree(value);
        } catch (IOException ex) {
            throw new AssertionError(ex);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
