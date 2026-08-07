package com.mindbridge.dailyquestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Integration tests for the daily question answer endpoint (G2-T06).
 *
 * Verifies Definition of Done §4:
 * - §4.1 Scale out of range → 400 (DoD §4.1)
 * - §4.2 Option belonging to another template → 400 (DoD §4.2)
 * - §4.3 Two answers for one assignment → 409 (DoD §4.3, immutable plan A)
 * - §4.4 History returns by local date (DoD §4.4)
 *
 * Plus:
 * - Ownership: alice can't submit answer for bob's assignment → 403
 * - Missing assignment → 404
 * - answerType mismatch with template questionType → 400
 * - Exactly-one-value violation → 400
 * - Unauthenticated → 401
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-daily-question.sql",
        "classpath:schema-daily-question-assignments.sql",
        "classpath:schema-daily-question-answers.sql",
        "classpath:schema-behavioral-events.sql",
        "classpath:schema-idempotency-keys.sql"
})
@DisplayName("Daily question answer integration")
class DailyQuestionAnswerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository answerRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository assignmentRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository templateRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository optionRepository;

    @Autowired
    private com.mindbridge.behavior.repository.BehavioralEventRepository behavioralEventRepository;

    @Autowired
    private com.mindbridge.dailyquestion.service.DailyQuestionTemplateService templateService;

    @BeforeEach
    void setUp() {
        seedMvpTemplates();
    }

    @AfterEach
    void cleanup() {
        behavioralEventRepository.deleteAll();
        answerRepository.deleteAll();
        assignmentRepository.deleteAll();
        optionRepository.deleteAll();
        templateRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- DoD §4.1: Scale out of range ---

    @Test
    @DisplayName("DoD §4.1 — SCALE answer with numericValue above scale_max → 400")
    void scaleOutOfRange_above_400() throws Exception {
        String token = registerUser("alice-scale-up", "UTC");

        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        String body = """
                {"answerType":"NUMERIC","numericValue":10}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("scale_max")));
    }

    @Test
    @DisplayName("DoD §4.1 — SCALE answer with numericValue below scale_min → 400")
    void scaleOutOfRange_below_400() throws Exception {
        String token = registerUser("alice-scale-down", "UTC");
        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        String body = """
                {"answerType":"NUMERIC","numericValue":0}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("scale_min")));
    }

    @Test
    @DisplayName("DoD §4.1 — SCALE answer at exact boundary (1 and 5) → 201")
    void scaleAtBoundary_201() throws Exception {
        String token = registerUser("alice-scale-boundary", "UTC");
        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        String body = """
                {"answerType":"NUMERIC","numericValue":1}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Submit 5 to a different template (ENERGY) to verify upper boundary works
        UUID energyAssignmentId = getAssignmentIdByCode(token, "ENERGY");
        String body2 = """
                {"answerType":"NUMERIC","numericValue":5}
                """;
        mockMvc.perform(post("/daily-checkins/" + energyAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("NUMBER (SLEEP) answer with numericValue above 24 → 400")
    void numberOutOfRange_400() throws Exception {
        String token = registerUser("alice-num", "UTC");
        UUID sleepAssignmentId = getAssignmentIdByCode(token, "SLEEP");

        String body = """
                {"answerType":"NUMERIC","numericValue":48}
                """;
        mockMvc.perform(post("/daily-checkins/" + sleepAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("scale_max")));
    }

    // --- DoD §4.2: Option belonging to another template ---

    @Test
    @DisplayName("DoD §4.2 — OPTION answer with value from MOOD submitted against STRESS (SCALE) → 400")
    void optionFromOtherTemplate_400() throws Exception {
        String token = registerUser("alice-opt-other", "UTC");
        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        // STRESS is SCALE — sending OPTION with "1" (which exists in MOOD options) → validation
        // should fail at the answerType-vs-questionType match check (SCALE ≠ OPTION).
        String body = """
                {"answerType":"OPTION","optionValue":"1"}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not match")));
    }

    @Test
    @DisplayName("DoD §4.2 — OPTION value not in template's options set → 400")
    void optionNotInTemplateOptions_400() throws Exception {
        String token = registerUser("alice-opt-bad", "UTC");
        UUID moodAssignmentId = getAssignmentIdByCode(token, "MOOD");

        // MOOD options are "1","2","3","4","5" — sending "9" should fail.
        String body = """
                {"answerType":"OPTION","optionValue":"9"}
                """;
        mockMvc.perform(post("/daily-checkins/" + moodAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not belong")));
    }

    @Test
    @DisplayName("Valid OPTION submit against MOOD (optionValue=\"3\") → 201")
    void validOption_201() throws Exception {
        String token = registerUser("alice-opt-ok", "UTC");
        UUID moodAssignmentId = getAssignmentIdByCode(token, "MOOD");

        String body = """
                {"answerType":"OPTION","optionValue":"3"}
                """;
        mockMvc.perform(post("/daily-checkins/" + moodAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerType").value("OPTION"))
                .andExpect(jsonPath("$.optionValue").value("3"))
                .andExpect(jsonPath("$.numericValue").doesNotExist())
                .andExpect(jsonPath("$.textValue").doesNotExist());
    }

    @Test
    @DisplayName("Valid TEXT submit against OPEN → 201")
    void validText_201() throws Exception {
        String token = registerUser("alice-text", "UTC");
        UUID openAssignmentId = getAssignmentIdByCode(token, "OPEN");

        String body = """
                {"answerType":"TEXT","textValue":"Hôm nay tôi cảm thấy ổn"}
                """;
        mockMvc.perform(post("/daily-checkins/" + openAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.answerType").value("TEXT"))
                .andExpect(jsonPath("$.textValue").value("Hôm nay tôi cảm thấy ổn"));
    }

    // --- DoD §4.3: Two answers for one assignment ---

    @Test
    @DisplayName("DoD §4.3 — Submit twice on same assignment → second is 409")
    void submitTwice_409() throws Exception {
        String token = registerUser("alice-twice", "UTC");
        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        String body = """
                {"answerType":"NUMERIC","numericValue":3}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"NUMERIC","numericValue":4}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECKIN_ANSWER_DUPLICATE"));

        // Only one row exists
        assertThat(answerRepository.count()).isEqualTo(1);
    }

    // --- answerType mismatch (additional safety) ---

    @Test
    @DisplayName("answerType NUMERIC against TEXT template (OPEN) → 400")
    void answerTypeMismatch_400() throws Exception {
        String token = registerUser("alice-mismatch", "UTC");
        UUID openAssignmentId = getAssignmentIdByCode(token, "OPEN");

        String body = """
                {"answerType":"NUMERIC","numericValue":5}
                """;
        mockMvc.perform(post("/daily-checkins/" + openAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not match")));
    }

    // --- Exactly-one-value violation ---

    @Test
    @DisplayName("Setting both numericValue and optionValue → 400")
    void multipleValues_400() throws Exception {
        String token = registerUser("alice-multi", "UTC");
        UUID stressAssignmentId = getAssignmentIdByCode(token, "STRESS");

        String body = """
                {"answerType":"NUMERIC","numericValue":3,"optionValue":"1"}
                """;
        mockMvc.perform(post("/daily-checkins/" + stressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Exactly one")));
    }

    // --- Ownership / auth ---

    @Test
    @DisplayName("alice submits answer for bob's assignment → 403")
    void crossUser_403() throws Exception {
        String aliceToken = registerUser("alice-cross", "UTC");
        String bobToken = registerUser("bob-cross", "UTC");

        // bob triggers assignment first
        UUID bobStressAssignmentId = getAssignmentIdByCode(bobToken, "STRESS");

        mockMvc.perform(post("/daily-checkins/" + bobStressAssignmentId + "/answer")
                        .header("Authorization", "Bearer " + aliceToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"NUMERIC","numericValue":3}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Submit answer for non-existent assignment → 404")
    void notFound_404() throws Exception {
        String token = registerUser("alice-404", "UTC");
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(post("/daily-checkins/" + randomId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"NUMERIC","numericValue":3}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST answer without token → 401")
    void noToken_401() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(post("/daily-checkins/" + randomId + "/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"NUMERIC","numericValue":3}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- DoD §4.4: History ---

    @Test
    @DisplayName("DoD §4.4 — GET /daily-checkins/history returns submitted answers grouped by date")
    void getHistory_returnsByDate() throws Exception {
        String token = registerUser("alice-history", "UTC");

        // Submit answers for 2 templates
        UUID stressId = getAssignmentIdByCode(token, "STRESS");
        UUID moodId = getAssignmentIdByCode(token, "MOOD");

        mockMvc.perform(post("/daily-checkins/" + stressId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"NUMERIC","numericValue":3}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/daily-checkins/" + moodId + "/answer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"answerType":"OPTION","optionValue":"4"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/daily-checkins/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").exists())
                .andExpect(jsonPath("$[0].timezone").value("UTC"))
                .andExpect(jsonPath("$[0].answers.length()").value(2));
    }

    @Test
    @DisplayName("Empty history (no answers yet) → 200, []")
    void getHistory_empty_200() throws Exception {
        String token = registerUser("alice-no-history", "UTC");

        mockMvc.perform(get("/daily-checkins/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /daily-checkins/history without token → 401")
    void getHistory_noToken_401() throws Exception {
        mockMvc.perform(get("/daily-checkins/history"))
                .andExpect(status().isUnauthorized());
    }

    // --- Helpers ---

    private String registerUser(String prefix, String timezone) throws Exception {
        String unique = prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
        String body = String.format(
                "{\"email\":\"%s@example.com\",\"password\":\"PassPass123!\",\"displayName\":\"%s\"}",
                unique, unique);
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        if (timezone != null && !"UTC".equals(timezone)) {
            User user = userRepository.findByEmailIgnoreCase(unique + "@example.com").orElseThrow();
            user.setTimezone(timezone);
            userRepository.save(user);
        }

        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("accessToken").asText();
    }

    /**
     * Helper: get today's assignments for the user, then return the assignmentId
     * whose templateCode matches `code`.
     */
    private UUID getAssignmentIdByCode(String token, String code) throws Exception {
        MvcResult result = mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        var array = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        for (var node : array) {
            if (code.equals(node.get("templateCode").asText())) {
                return UUID.fromString(node.get("assignmentId").asText());
            }
        }
        throw new IllegalStateException("Assignment for code " + code + " not found");
    }

    private void seedMvpTemplates() {
        if (templateRepository.count() > 0) {
            return;
        }
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "STRESS", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Stress level?", null));
        templateService.updateByCode("STRESS",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Stress level?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("STRESS", "1", "5");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "MOOD", com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                "Mood?",
                List.of(
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Very bad", 1),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "Bad", 2),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "OK", 3),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("4", "Good", 4),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("5", "Great", 5)
                )));
        templateService.updateByCode("MOOD",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                        "Mood?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED,
                        List.of(
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Very bad", 1),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "Bad", 2),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "OK", 3),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("4", "Good", 4),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("5", "Great", 5)
                        )));

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "SLEEP", com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                "Sleep hours?", null));
        templateService.updateByCode("SLEEP",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                        "Sleep hours?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("SLEEP", "0", "24");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "ENERGY", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Energy?", null));
        templateService.updateByCode("ENERGY",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Energy?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("ENERGY", "1", "5");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "OPEN", com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                "Share something?", null));
        templateService.updateByCode("OPEN",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                        "Share something?", com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
    }

    private void setScaleRange(String code, String min, String max) {
        var tpl = templateRepository.findTopByCodeOrderByVersionDesc(code).orElseThrow();
        tpl.setScaleRange(new BigDecimal(min), new BigDecimal(max));
        templateRepository.save(tpl);
    }
}
