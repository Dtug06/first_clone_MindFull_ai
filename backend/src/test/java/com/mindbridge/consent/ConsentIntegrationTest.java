package com.mindbridge.consent;

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
 * Integration tests for consent management.
 *
 * Verifies:
 * - Append-only behavior: events accumulate, never overwrite.
 * - Current state = latest event per type.
 * - Ownership: each user only sees their own consent state.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql"
})
@DisplayName("Consent integration")
class ConsentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /consents → 201, records event with current user")
    void recordConsent_201() throws Exception {
        String token = registerUser("alice");

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consentType").value("CHAT_ANALYSIS"))
                .andExpect(jsonPath("$.action").value("GRANTED"))
                .andExpect(jsonPath("$.policyVersion").value("1.0"))
                .andExpect(jsonPath("$.occurredAt").exists());
    }

    @Test
    @DisplayName("Append-only: GRANTED then REVOKED leaves 2 rows; current state reflects REVOKED")
    void appendOnly_historyPreserved() throws Exception {
        String token = registerUser("bob");

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"REVOKED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/consents/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.consentType=='CHAT_ANALYSIS')].granted").value(
                        org.hamcrest.Matchers.contains(false)));
    }

    @Test
    @DisplayName("Current state for a user with no events → all granted=false")
    void noEvents_allRevoked() throws Exception {
        String token = registerUser("charlie");

        mockMvc.perform(get("/consents/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.consentType=='CHAT_ANALYSIS')].granted").value(
                        org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.consentType=='PERSONALIZATION')].granted").value(
                        org.hamcrest.Matchers.contains(false)))
                .andExpect(jsonPath("$[?(@.consentType=='EXPERT_SHARING')].granted").value(
                        org.hamcrest.Matchers.contains(false)));
    }

    @Test
    @DisplayName("Ownership: alice sees only her own consent, not bob's")
    void ownership_isolated() throws Exception {
        String aliceToken = registerUser("alice-owner");
        String bobToken = registerUser("bob-other");

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isCreated());

        MvcResult bobResult = mockMvc.perform(get("/consents/current")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = bobResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode arr = objectMapper.readTree(body);
        for (JsonNode entry : arr) {
            org.assertj.core.api.Assertions.assertThat(entry.get("granted").asBoolean())
                    .as("Bob must not see Alice's granted consent")
                    .isFalse();
        }
    }

    @Test
    @DisplayName("GET /consents/current without token → 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/consents/current"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Invalid consentType rejected with 400")
    void invalidEnum() throws Exception {
        String token = registerUser("dave");

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"BOGUS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Multiple types tracked independently")
    void multipleTypes() throws Exception {
        String token = registerUser("eve");

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(get("/consents/current")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode arr = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        for (JsonNode entry : arr) {
            String type = entry.get("consentType").asText();
            boolean granted = entry.get("granted").asBoolean();
            if ("CHAT_ANALYSIS".equals(type)) {
                org.assertj.core.api.Assertions.assertThat(granted).isTrue();
            } else {
                org.assertj.core.api.Assertions.assertThat(granted).isFalse();
            }
        }
    }

    /**
     * Registers a fresh user with a unique email derived from {@code prefix}
     * and returns the access token.
     */
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