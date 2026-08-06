package com.mindbridge.behavior;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.dto.AuthResponse;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.domain.BehavioralEvent;
import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import com.mindbridge.behavior.service.BehavioralEventService;
import com.mindbridge.chat.domain.ChatSession;
import com.mindbridge.chat.repository.ChatSessionRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Integration tests for the Behavioral Event Log (G2-T07).
 *
 * Verifies Definition of Done §4:
 * - §4.1 "Mỗi hành động nghiệp vụ tạo đúng một event cần thiết"
 * - §4.2 "Event có source_id để truy ngược bản ghi gốc"
 * - §4.3 "Retry request không tạo event trùng ngoài dự kiến"
 *
 * Plus:
 * - properties MUST NOT contain raw content (only metadata) — DoD spirit + G2-T07 §2.3
 * - schema_version defaults to 1
 * - record() never propagates exceptions (defensive guard — parent action must succeed)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql",
        "classpath:schema-chat-sessions.sql",
        "classpath:schema-conversation-messages.sql",
        "classpath:schema-daily-question.sql",
        "classpath:schema-daily-question-assignments.sql",
        "classpath:schema-daily-question-answers.sql",
        "classpath:schema-behavioral-events.sql",
        "classpath:schema-idempotency-keys.sql"
})
@DisplayName("Behavioral event log integration")
class BehavioralEventServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private BehavioralEventRepository behavioralEventRepository;

    @Autowired
    private BehavioralEventService behavioralEventService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @AfterEach
    void cleanup() {
        behavioralEventRepository.deleteAll();
        chatSessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- DoD §4.1: Each business action emits exactly one event ---

    @Test
    @DisplayName("DoD §4.1 — POST /chat/sessions → CHAT_SESSION_STARTED event recorded")
    void createSession_recordsChatSessionStarted() throws Exception {
        String token = registerUser("alice-session");
        UUID sessionId = createChatSession(token, "{\"title\":\"Test\"}");

        var events = behavioralEventRepository.findAll();
        assertThat(events).hasSize(1);

        BehavioralEvent e = events.get(0);
        assertThat(e.getEventType()).isEqualTo(BehavioralEventType.CHAT_SESSION_STARTED);
        assertThat(e.getSourceType()).isEqualTo(SourceType.CHAT_SESSION);
        assertThat(e.getSourceId()).isEqualTo(sessionId);
        assertThat(e.getUser().getId()).isNotNull();
    }

    @Test
    @DisplayName("DoD §4.1 — POST /chat/sessions/{id}/messages → CHAT_MESSAGE_SENT event recorded")
    void sendMessage_recordsChatMessageSent() throws Exception {
        String token = registerUser("alice-msg");
        UUID sessionId = createChatSession(token, "{}");
        sendMessage(token, sessionId, "{\"content\":\"Hello\"}");

        var events = behavioralEventRepository.findAll();
        BehavioralEvent e = events.stream()
                .filter(x -> x.getEventType() == BehavioralEventType.CHAT_MESSAGE_SENT)
                .findFirst()
                .orElseThrow();
        assertThat(e.getSourceType()).isEqualTo(SourceType.CONVERSATION_MESSAGE);
        assertThat(e.getSourceId()).isNotNull();
        assertThat(e.getSourceId()).isNotEqualTo(sessionId);
    }

    @Test
    @DisplayName("DoD §4.1 — POST /daily-checkins/{id}/answer → DAILY_CHECKIN_COMPLETED event recorded")
    void submitAnswer_recordsDailyCheckinCompleted() throws Exception {
        String token = registerUser("alice-checkin");
        seedMvpTemplates();

        // trigger assignment creation
        MvcResult todayResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/daily-checkins/today")
                                .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(todayResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        UUID stressAssignmentId = null;
        for (JsonNode n : array) {
            if ("STRESS".equals(n.get("templateCode").asText())) {
                stressAssignmentId = UUID.fromString(n.get("assignmentId").asText());
                break;
            }
        }
        assertThat(stressAssignmentId).isNotNull();

        // submit answer
        mockMvc.perform(MockMvcRequestBuilders.post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerType\":\"NUMERIC\",\"numericValue\":3}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        var events = behavioralEventRepository.findAll();
        BehavioralEvent e = events.stream()
                .filter(x -> x.getEventType() == BehavioralEventType.DAILY_CHECKIN_COMPLETED)
                .findFirst()
                .orElseThrow();
        assertThat(e.getSourceType()).isEqualTo(SourceType.DAILY_QUESTION_ANSWER);
        // source_id == assignment_id (per G2-T07 plan §2.6)
        assertThat(e.getSourceId()).isEqualTo(stressAssignmentId);
    }

    // --- DoD §4.2: Event source_id matches the original record ---

    @Test
    @DisplayName("DoD §4.2 — chat session: source_id matches ChatSession.id (back-traceable)")
    void chatSession_sourceIdMatchesRecord() throws Exception {
        String token = registerUser("alice-trace");
        UUID sessionId = createChatSession(token, "{}");

        var e = behavioralEventRepository.findBySourceTypeAndSourceIdAndEventType(
                SourceType.CHAT_SESSION, sessionId, BehavioralEventType.CHAT_SESSION_STARTED);
        assertThat(e).isPresent();
        assertThat(e.get().getSourceId()).isEqualTo(sessionId);

        ChatSession original = chatSessionRepository.findById(sessionId).orElseThrow();
        assertThat(original.getId()).isEqualTo(e.get().getSourceId());
    }

    @Test
    @DisplayName("DoD §4.2 — daily check-in: source_id == assignment.id (assignment is anchor)")
    void checkinCompleted_sourceIdIsAssignmentId() throws Exception {
        String token = registerUser("alice-anchor");
        seedMvpTemplates();

        MvcResult todayResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/daily-checkins/today")
                                .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(todayResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        UUID sleepId = null;
        for (JsonNode n : array) {
            if ("SLEEP".equals(n.get("templateCode").asText())) {
                sleepId = UUID.fromString(n.get("assignmentId").asText());
                break;
            }
        }
        assertThat(sleepId).isNotNull();

        mockMvc.perform(MockMvcRequestBuilders.post("/daily-checkins/" + sleepId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerType\":\"NUMERIC\",\"numericValue\":7}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        var e = behavioralEventRepository.findBySourceTypeAndSourceIdAndEventType(
                SourceType.DAILY_QUESTION_ANSWER, sleepId, BehavioralEventType.DAILY_CHECKIN_COMPLETED);
        assertThat(e).isPresent();
    }

    // --- DoD §4.3: Retry does NOT create duplicate events ---

    @Test
    @DisplayName("DoD §4.3 — double-record() with same natural key: only 1 row, both calls return same event")
    void doubleRecord_isIdempotent() {
        String unique = "alice-idem-" + UUID.randomUUID().toString().substring(0, 8);
        User user = createUser(unique);
        UUID sourceId = UUID.randomUUID();

        BehavioralEvent first = behavioralEventService.record(
                user.getId(), BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION, sourceId,
                Map.of("title_present", true));
        BehavioralEvent second = behavioralEventService.record(
                user.getId(), BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION, sourceId,
                Map.of("title_present", false));

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.getId()).isEqualTo(second.getId());
        assertThat(behavioralEventRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("DoD §4.3 — submit answer retry: existing UNIQUE on assignment_id blocks answer + no 2nd event")
    void submitAnswerTwice_createsOneEvent() throws Exception {
        String token = registerUser("alice-once");
        seedMvpTemplates();

        MvcResult todayResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/daily-checkins/today")
                                .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(todayResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        final UUID[] energyHolder = new UUID[1];
        for (JsonNode n : array) {
            if ("ENERGY".equals(n.get("templateCode").asText())) {
                energyHolder[0] = UUID.fromString(n.get("assignmentId").asText());
                break;
            }
        }
        UUID energyId = energyHolder[0];
        assertThat(energyId).isNotNull();

        mockMvc.perform(MockMvcRequestBuilders.post("/daily-checkins/" + energyId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerType\":\"NUMERIC\",\"numericValue\":4}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        // retry: 409 → no event recorded
        mockMvc.perform(MockMvcRequestBuilders.post("/daily-checkins/" + energyId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerType\":\"NUMERIC\",\"numericValue\":5}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());

        long completedCount = behavioralEventRepository.findAll().stream()
                .filter(e -> e.getEventType() == BehavioralEventType.DAILY_CHECKIN_COMPLETED)
                .filter(e -> energyId.equals(e.getSourceId()))
                .count();
        assertThat(completedCount).isEqualTo(1);
    }

    // --- Properties safety: NO raw content ---

    @Test
    @DisplayName("CHAT_MESSAGE_SENT properties contains message_length, role, was_redacted — NEVER raw content")
    void chatMessageEvent_propertiesAreSafe() throws Exception {
        String token = registerUser("alice-safe");
        UUID sessionId = createChatSession(token, "{}");
        sendMessage(token, sessionId, "{\"content\":\"My super secret worry\"}");

        BehavioralEvent e = behavioralEventRepository.findAll().stream()
                .filter(x -> x.getEventType() == BehavioralEventType.CHAT_MESSAGE_SENT)
                .findFirst().orElseThrow();

        String json = e.getProperties();
        assertThat(json).contains("message_length");
        assertThat(json).contains("\"role\":\"USER\"");
        assertThat(json).contains("\"was_redacted\"");
        // CRITICAL: raw content must not be present in any form
        assertThat(json).doesNotContain("secret");
        assertThat(json).doesNotContain("worry");
        assertThat(json).doesNotContain("super");
    }

    @Test
    @DisplayName("DAILY_CHECKIN_COMPLETED properties contains answer_type + template_code + assignment_id, NOT numeric value")
    void checkinEvent_propertiesAreSafe() throws Exception {
        String token = registerUser("alice-checkin-safe");
        seedMvpTemplates();

        MvcResult todayResult = mockMvc.perform(
                        MockMvcRequestBuilders.get("/daily-checkins/today")
                                .header("Authorization", "Bearer " + token))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn();
        JsonNode array = objectMapper.readTree(todayResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        UUID moodId = null;
        for (JsonNode n : array) {
            if ("MOOD".equals(n.get("templateCode").asText())) {
                moodId = UUID.fromString(n.get("assignmentId").asText());
                break;
            }
        }

        mockMvc.perform(MockMvcRequestBuilders.post("/daily-checkins/" + moodId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"answerType\":\"OPTION\",\"optionValue\":\"3\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());

        BehavioralEvent e = behavioralEventRepository.findAll().stream()
                .filter(x -> x.getEventType() == BehavioralEventType.DAILY_CHECKIN_COMPLETED)
                .findFirst().orElseThrow();

        String json = e.getProperties();
        assertThat(json).contains("\"answer_type\":\"OPTION\"");
        assertThat(json).contains("\"template_code\":\"MOOD\"");
        assertThat(json).contains("\"assignment_id\"");
        // CRITICAL: not present
        assertThat(json).doesNotContain("\"option_value\"");
        assertThat(json).doesNotContain("\"3\"");
    }

    // --- Defensive guard: event failure must not break parent action ---

    @Test
    @DisplayName("Defensive: record() with non-existent userId returns null instead of throwing")
    void recordWithMissingUser_doesNotPropagate() {
        BehavioralEvent result = behavioralEventService.record(
                UUID.randomUUID(), BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION, UUID.randomUUID(),
                Map.of("title_present", true));

        assertThat(result).isNull();
        assertThat(behavioralEventRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("Defensive: record() with null args returns null instead of throwing")
    void recordWithNullArgs_doesNotPropagate() {
        BehavioralEvent result = behavioralEventService.record(
                null, BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION, UUID.randomUUID(), null);

        assertThat(result).isNull();
        assertThat(behavioralEventRepository.count()).isEqualTo(0);
    }

    // --- schema_version ---

    @Test
    @DisplayName("schema_version defaults to 1 on insert")
    void schemaVersion_defaultsToOne() throws Exception {
        String token = registerUser("alice-schema");
        createChatSession(token, "{}");

        BehavioralEvent e = behavioralEventRepository.findAll().get(0);
        assertThat(e.getSchemaVersion()).isEqualTo((short) 1);
    }

    // --- Helpers ---

    private String registerUser(String prefix) throws Exception {
        String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
                "{\"email\":\"%s@example.com\",\"password\":\"PassPass123!\",\"displayName\":\"%s\"}",
                unique, unique);
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        AuthResponse auth = objectMapper.readValue(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8), AuthResponse.class);
        return auth.accessToken();
    }

    private UUID createChatSession(String token, String json) throws Exception {
        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.post("/chat/sessions")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                .andReturn();
        JsonNode n = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return UUID.fromString(n.get("id").asText());
    }

    private void sendMessage(String token, UUID sessionId, String json) throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/chat/sessions/" + sessionId + "/messages")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
    }

    private User createUser(String prefix) {
        // Register via the actual /auth/register endpoint to mirror real user shape,
        // then look up the user by email for direct test access.
        try {
            String body = String.format(
                    "{\"email\":\"%s@example.com\",\"password\":\"PassPass123!\",\"displayName\":\"%s\"}",
                    prefix, prefix);
            mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated());
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test user", e);
        }
        return userRepository.findByEmailIgnoreCase(prefix + "@example.com").orElseThrow();
    }

    /**
     * Seeds MVP daily-question templates for tests that need check-in endpoints.
     * Mirrors the seed in DailyQuestionAssignmentIntegrationTest.
     */
    private void seedMvpTemplates() {
        com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository templateRepository =
                beanFactoryGet(com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository.class);
        com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository optionRepository =
                beanFactoryGet(com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository.class);
        if (templateRepository.count() > 0) {
            return;
        }
        com.mindbridge.dailyquestion.service.DailyQuestionTemplateService templateService =
                beanFactoryGet(com.mindbridge.dailyquestion.service.DailyQuestionTemplateService.class);

        // STRESS
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "STRESS", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Stress level?", null));
        templateService.updateByCode("STRESS",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Stress level?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange(templateRepository, "STRESS", "1", "5");

        // MOOD with 5 options
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "MOOD", com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                "Mood?",
                List.of(
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Bad", 1),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "OK", 2),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "Good", 3)
                )));
        templateService.updateByCode("MOOD",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                        "Mood?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED,
                        List.of(
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Bad", 1),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "OK", 2),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "Good", 3)
                        )));

        // SLEEP
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "SLEEP", com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                "Sleep hours?", null));
        templateService.updateByCode("SLEEP",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                        "Sleep hours?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange(templateRepository, "SLEEP", "0", "24");

        // ENERGY
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "ENERGY", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Energy?", null));
        templateService.updateByCode("ENERGY",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Energy?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange(templateRepository, "ENERGY", "1", "5");

        // OPEN
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "OPEN", com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                "Share?", null));
        templateService.updateByCode("OPEN",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                        "Share?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
    }

    private void setScaleRange(
            com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository repo,
            String code, String min, String max) {
        var tpl = repo.findTopByCodeOrderByVersionDesc(code).orElseThrow();
        tpl.setScaleRange(new BigDecimal(min), new BigDecimal(max));
        repo.save(tpl);
    }

    private <T> T beanFactoryGet(Class<T> clazz) {
        return webApplicationContext.getBean(clazz);
    }
}