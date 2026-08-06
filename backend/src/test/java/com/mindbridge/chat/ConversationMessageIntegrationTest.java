package com.mindbridge.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for conversation message management.
 *
 * Verifies (Definition of Done):
 * - Send message and read back in correct order.
 * - Cannot inject message into another user's session.
 * - Pagination returns all N messages without duplicates or missing items.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql",
        "classpath:schema-chat-sessions.sql",
        "classpath:schema-conversation-messages.sql",
        "classpath:schema-ai-analysis-runs.sql",
        "classpath:schema-chat-analysis-results.sql",
        "classpath:schema-behavioral-events.sql",
        "classpath:schema-idempotency-keys.sql"
})
@DisplayName("Conversation message integration")
class ConversationMessageIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Send and read back in order ---

    @Test
    @DisplayName("POST message → 201, role=USER, analysisStatus=NOT_REQUESTED")
    void sendMessage_201() throws Exception {
        String token = registerUser("alice");
        UUID sessionId = createSession(token, "Test session");

        mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello, I need someone to talk to.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.content").value("Hello, I need someone to talk to."))
                .andExpect(jsonPath("$.analysisStatus").value("NOT_REQUESTED"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Send message and read back: message appears in list in correct order")
    void sendAndReadBack_orderPreserved() throws Exception {
        String token = registerUser("bob");
        UUID sessionId = createSession(token, "Bob's session");

        // Send first message
        MvcResult first = mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"First message\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstId = parseId(first);

        Thread.sleep(10); // ensure timestamps differ

        // Send second message
        MvcResult second = mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Second message\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID secondId = parseId(second);

        // Read back list
        MvcResult list = mockMvc.perform(get("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode items = parseItems(list);
        assertThat(items.size()).isEqualTo(2);
        assertThat(items.get(0).get("id").asText()).isEqualTo(firstId.toString());
        assertThat(items.get(0).get("content").asText()).isEqualTo("First message");
        assertThat(items.get(1).get("id").asText()).isEqualTo(secondId.toString());
        assertThat(items.get(1).get("content").asText()).isEqualTo("Second message");
    }

    // --- Pagination ---

    @Test
    @DisplayName("Pagination: N messages, page=0 size=2, then page=1 — all N returned, no duplicates")
    void pagination_noDuplicatesNoMissing() throws Exception {
        String token = registerUser("charlie");
        UUID sessionId = createSession(token, "Pagination test");
        int totalMessages = 5;
        int pageSize = 2;

        // Send 5 messages
        for (int i = 0; i < totalMessages; i++) {
            mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Message " + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        // Collect all message IDs across pages
        java.util.Set<String> allIds = new java.util.HashSet<>();
        int page = 0;
        while (true) {
            MvcResult pageResult = mockMvc.perform(get("/chat/sessions/" + sessionId + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(pageSize)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode resp = objectMapper.readTree(
                    pageResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
            long totalElements = resp.get("totalElements").asLong();
            JsonNode items = resp.get("items");

            for (JsonNode item : items) {
                allIds.add(item.get("id").asText());
            }

            int totalPages = resp.get("totalPages").asInt();
            if (page >= totalPages - 1) {
                break;
            }
            page++;
        }

        assertThat(allIds.size())
                .as("Must return exactly " + totalMessages + " unique messages, no duplicates")
                .isEqualTo(totalMessages);
    }

    @Test
    @DisplayName("Pagination fields: page, size, totalElements, totalPages present")
    void pagination_fields() throws Exception {
        String token = registerUser("dave");
        UUID sessionId = createSession(token, "Fields test");

        // Send 3 messages
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"Msg " + i + "\"}"))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items.length()").value(3));
    }

    // --- Ownership: cross-user injection blocked ---

    @Test
    @DisplayName("Cannot send message to another user's session → 403")
    void sendMessage_crossUser_forbidden_403() throws Exception {
        String aliceToken = registerUser("alice-cross");
        String bobToken = registerUser("bob-cross");

        UUID bobSessionId = createSession(bobToken, "Bob's private session");

        mockMvc.perform(post("/chat/sessions/" + bobSessionId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Unauthorized message\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Cannot list messages from another user's session → 403")
    void listMessages_crossUser_forbidden_403() throws Exception {
        String aliceToken = registerUser("alice-list");
        String bobToken = registerUser("bob-list");

        UUID bobSessionId = createSession(bobToken, "Bob's session");

        mockMvc.perform(get("/chat/sessions/" + bobSessionId + "/messages")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // --- Not found ---

    @Test
    @DisplayName("Send message to non-existent session → 404")
    void sendMessage_sessionNotFound_404() throws Exception {
        String token = registerUser("eve");

        mockMvc.perform(post("/chat/sessions/" + UUID.randomUUID() + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Test\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // --- Authentication ---

    @Test
    @DisplayName("POST message without token → 401")
    void sendMessage_noToken_401() throws Exception {
        mockMvc.perform(post("/chat/sessions/" + UUID.randomUUID() + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET messages without token → 401")
    void listMessages_noToken_401() throws Exception {
        mockMvc.perform(get("/chat/sessions/" + UUID.randomUUID() + "/messages"))
                .andExpect(status().isUnauthorized());
    }

    // --- Empty session ---

    @Test
    @DisplayName("List messages from empty session → 200, empty array")
    void listMessages_empty_200() throws Exception {
        String token = registerUser("frank");
        UUID sessionId = createSession(token, "Empty session");

        mockMvc.perform(get("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // --- Validation ---

    @Test
    @DisplayName("Send empty content → 400")
    void sendMessage_emptyContent_400() throws Exception {
        String token = registerUser("grace");
        UUID sessionId = createSession(token, "Validation test");

        mockMvc.perform(post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- Helpers ---

    private String registerUser(String prefix) throws Exception {
        String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
                "{\"email\":\"%s@example.com\",\"password\":\"PassPass123!\",\"displayName\":\"%s\"}",
                unique, unique);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    private UUID createSession(String token, String title) throws Exception {
        String body = (title != null) ? "{\"title\":\"" + title + "\"}" : "{}";
        MvcResult result = mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());
    }

    private UUID parseId(MvcResult result) throws Exception {
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());
    }

    private JsonNode parseItems(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("items");
    }
}
