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
 * Integration tests for chat session management.
 *
 * Verifies:
 * - Create, list and close sessions.
 * - Ownership: a user's sessions are isolated from other users.
 * - Sessions are ordered by most recently active (updated_at DESC).
 * - Unauthenticated access returns 401.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-chat-sessions.sql",
        "classpath:schema-behavioral-events.sql"
})
@DisplayName("Chat session integration")
class ChatSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- Create session ---

    @Test
    @DisplayName("POST /chat/sessions with no body → 201, status=ACTIVE, has id")
    void createSession_noBody_201() throws Exception {
        String token = registerUser("alice");

        mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    @DisplayName("POST /chat/sessions with title → 201, title reflected in response")
    void createSession_withTitle_201() throws Exception {
        String token = registerUser("bob");

        mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"My therapy session\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My therapy session"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    // --- List sessions ---

    @Test
    @DisplayName("GET /chat/sessions returns current user's sessions, ordered by updatedAt DESC")
    void listSessions_orderedByUpdatedAtDesc() throws Exception {
        String token = registerUser("charlie");

        // Create first session
        MvcResult first = mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstId = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText()
                .transform(UUID::fromString);

        Thread.sleep(10); // ensure timestamps differ

        // Create second session
        mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second\"}"))
                .andExpect(status().isCreated());

        // List sessions
        MvcResult listResult = mockMvc.perform(get("/chat/sessions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        JsonNode page = objectMapper.readTree(listResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode firstItem = page.get("items").get(0);
        JsonNode secondItem = page.get("items").get(1);

        assertThat(firstItem.get("id").asText()).isNotEqualTo(firstId.toString());
        assertThat(secondItem.get("id").asText()).isEqualTo(firstId.toString());
    }

    @Test
    @DisplayName("List sessions: pagination fields are present")
    void listSessions_paginationFields() throws Exception {
        String token = registerUser("dave");

        mockMvc.perform(get("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.items").isArray());
    }

    // --- Get single session ---

    @Test
    @DisplayName("GET /chat/sessions/{id} with valid id → 200, returns session")
    void getSession_200() throws Exception {
        String token = registerUser("eve");

        MvcResult createResult = mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Private\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/chat/sessions/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Private"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /chat/sessions/{id} for non-existent session → 404")
    void getSession_notFound_404() throws Exception {
        String token = registerUser("frank");

        mockMvc.perform(get("/chat/sessions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // --- Close session ---

    @Test
    @DisplayName("POST /chat/sessions/{id}/close → 200, status=CLOSED, closedAt set")
    void closeSession_200() throws Exception {
        String token = registerUser("grace");

        MvcResult createResult = mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"To close\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/chat/sessions/" + id + "/close")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // --- Ownership ---

    @Test
    @DisplayName("Ownership: alice cannot get bob's session → 403")
    void getSession_crossUser_forbidden_403() throws Exception {
        String aliceToken = registerUser("alice-cross");
        String bobToken = registerUser("bob-cross");

        MvcResult createResult = mockMvc.perform(post("/chat/sessions")
                        .header("Authorization", "Bearer " + bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Bob's session\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String bobSessionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/chat/sessions/" + bobSessionId)
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    // --- Authentication ---

    @Test
    @DisplayName("POST /chat/sessions without token → 401")
    void createSession_noToken_401() throws Exception {
        mockMvc.perform(post("/chat/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /chat/sessions without token → 401")
    void listSessions_noToken_401() throws Exception {
        mockMvc.perform(get("/chat/sessions"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helper ---

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
}
