package com.mindbridge.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditActions;
import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.domain.entity.AuditLog;
import com.mindbridge.common.repository.AuditLogRepository;
import com.mindbridge.consent.repository.ConsentEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for audit logging.
 *
 * Verifies:
 * - Login failure produces an AUTH/LOGIN_FAILED row with an email hash.
 * - Consent grant produces a CONSENT/CONSENT_GRANTED row.
 * - requestId from the X-Request-Id header is stored on the row.
 * - Successful login does NOT produce an audit row.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql",
        "classpath:schema-audit.sql"
})
@DisplayName("Audit integration")
class AuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ConsentEventRepository consentEventRepository;

    @AfterEach
    void cleanup() {
        // Delete in FK-safe order: audit → consent → users.
        auditLogRepository.deleteAll();
        consentEventRepository.deleteAll();
        userRepository.deleteAll();
        MDC.clear();
    }

    @Test
    @DisplayName("Login failure produces an AUTH audit row with email hash + X-Request-Id")
    void loginFailure_auditRow() throws Exception {
        String requestId = "itest-" + UUID.randomUUID();

        mockMvc.perform(post("/auth/login")
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com","password":"whatever123!"}
                                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());

        var rows = auditLogRepository.findByRequestId(requestId);
        assertThat(rows).hasSize(1);
        AuditLog row = rows.get(0);
        assertThat(row.getCategory()).isEqualTo(AuditCategory.AUTH);
        assertThat(row.getAction()).isEqualTo(AuditActions.LOGIN_FAILED);
        assertThat(row.getActorType()).isEqualTo(AuditActorType.ANONYMOUS);
        assertThat(row.getActorId()).isNull();
        assertThat(row.getMetadata()).contains("emailHash").doesNotContain("nobody@example.com");
    }

    @Test
    @DisplayName("Successful login does NOT produce an audit row")
    void loginSuccess_noAuditRow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "audit-ok-" + suffix + "@example.com";
        String password = "PassPass123!";
        registerUser(email, password);

        auditLogRepository.deleteAll();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format(
                                "{\"email\":\"%s\",\"password\":\"%s\"}", email, password)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        assertThat(auditLogRepository.findAll()).noneMatch(r -> r.getCategory() == AuditCategory.AUTH);
    }

    @Test
    @DisplayName("Consent grant produces a CONSENT audit row tied to the request")
    void consentGrant_auditRow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "audit-consent-" + suffix + "@example.com";
        String token = registerUser(email);

        String requestId = "audit-consent-" + UUID.randomUUID();

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"CHAT_ANALYSIS","action":"GRANTED","policyVersion":"1.0"}
                                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        var rows = auditLogRepository.findByRequestId(requestId);
        assertThat(rows).hasSize(1);
        AuditLog row = rows.get(0);
        assertThat(row.getCategory()).isEqualTo(AuditCategory.CONSENT);
        assertThat(row.getAction()).isEqualTo(AuditActions.CONSENT_GRANTED);
        assertThat(row.getActorType()).isEqualTo(AuditActorType.USER);
        assertThat(row.getActorId()).isNotNull();
        assertThat(row.getSubjectId()).isNotNull();
        assertThat(row.getMetadata()).contains("CHAT_ANALYSIS");
    }

    @Test
    @DisplayName("Consent revoke produces a CONSENT_REVOKED audit row")
    void consentRevoke_auditRow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "audit-revoke-" + suffix + "@example.com";
        String token = registerUser(email);

        String requestId = "audit-revoke-" + UUID.randomUUID();

        mockMvc.perform(post("/consents")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Request-Id", requestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentType":"PERSONALIZATION","action":"REVOKED","policyVersion":"1.0"}
                                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        var rows = auditLogRepository.findByRequestId(requestId);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getAction()).isEqualTo(AuditActions.CONSENT_REVOKED);
    }

    @Test
    @DisplayName("Response header carries the echo X-Request-Id")
    void responseHeaderEcho() throws Exception {
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .get("/actuator/health"))
                .andReturn();
        assertThat(result.getResponse().getHeader("X-Request-Id")).isNotBlank();
    }

    private String registerUser(String email) throws Exception {
        return registerUser(email, "PassPass123!");
    }

    private String registerUser(String email, String password) throws Exception {
        String body = String.format(
                "{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\"}",
                email, password, email);
        MvcResult result = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return json.get("accessToken").asText();
    }
}