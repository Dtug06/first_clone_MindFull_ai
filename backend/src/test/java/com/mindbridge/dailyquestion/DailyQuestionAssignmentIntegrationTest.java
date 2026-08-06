package com.mindbridge.dailyquestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for the daily question assignment endpoint.
 *
 * Verifies DoD for G2-T05:
 * - §4.1: each user receives the correct set of questions for their local date.
 * - §4.2: subsequent calls (simulating a re-run job) do not create duplicate assignments.
 * - §4.3: the frontend-today endpoint returns the expected JSON shape.
 * - Cross-user isolation: user A cannot see user B's assignments.
 * - Timezone resilience: changing the timezone does not create a duplicate assignment
 *   for the same calendar day.
 * - Historical consistency: a new template version published after assignment does
 *   not retroactively change the user's already-assigned prompt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-daily-question.sql",
        "classpath:schema-daily-question-assignments.sql",
        "classpath:schema-daily-question-answers.sql",
        "classpath:schema-behavioral-events.sql"
})
@DisplayName("Daily question assignment integration")
class DailyQuestionAssignmentIntegrationTest {

    /**
     * Freezes {@link Clock} to a fixed UTC noon so the test is not flaky at
     * the UTC ↔ Asia/Ho_Chi_Minh day boundary. Using noon UTC means the
     * local date is the same in UTC, Asia/Ho_Chi_Minh (UTC+7), America/Los_Angeles
     * (UTC-7/-8) and similar zones — the {@code userChangesTimezone_sameDate_noDuplicate}
     * test can therefore flip timezones safely without producing a second
     * calendar day.
     */
    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        Clock frozenClock() {
            // 2026-06-15T12:00:00Z — a fixed instant far from any DST boundary.
            return Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // Seed 5 MVP templates (matching V6 seeds) for each test so the
        // assignment service has APPROVED templates to assign.
        seedMvpTemplates();
    }

    @AfterEach
    void cleanup() {
        // Clean in dependency order: events → answers → assignments → options → templates → users
        behavioralEventRepository.deleteAll();
        answerRepository.deleteAll();
        assignmentRepository.deleteAll();
        optionRepository.deleteAll();
        templateRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository assignmentRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository answerRepository;

    @Autowired
    private com.mindbridge.behavior.repository.BehavioralEventRepository behavioralEventRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository templateRepository;

    @Autowired
    private com.mindbridge.dailyquestion.repository.DailyQuestionOptionRepository optionRepository;

    @Autowired
    private com.mindbridge.dailyquestion.service.DailyQuestionTemplateService templateService;

    // --- Happy path ---

    @Test
    @DisplayName("GET /daily-checkins/today → 200, creates 5 assignments on first call")
    void firstCall_creates5Assignments() throws Exception {
        String token = registerUser("alice", "UTC");

        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].templateCode").exists())
                .andExpect(jsonPath("$[0].questionType").exists())
                .andExpect(jsonPath("$[0].prompt").exists())
                .andExpect(jsonPath("$[0].assignedForDate").exists())
                .andExpect(jsonPath("$[0].assignmentId").exists())
                .andExpect(jsonPath("$[0].answered").value(false));
    }

    @Test
    @DisplayName("GET /daily-checkins/today a second time → same 5 assignments, no duplicates")
    void secondCall_returnsSameAssignments_noDuplicates() throws Exception {
        String token = registerUser("alice", "UTC");

        MvcResult first = mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn();
        List<String> firstIds = extractAssignmentIds(first);

        MvcResult second = mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andReturn();
        List<String> secondIds = extractAssignmentIds(second);

        assertThat(secondIds).containsExactlyInAnyOrderElementsOf(firstIds);
        assertThat(assignmentRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET /daily-checkins/today → templates are ordered by code")
    void assignmentsOrderedByCode() throws Exception {
        String token = registerUser("alice", "UTC");

        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].templateCode").value("ENERGY"))
                .andExpect(jsonPath("$[1].templateCode").value("MOOD"))
                .andExpect(jsonPath("$[2].templateCode").value("OPEN"))
                .andExpect(jsonPath("$[3].templateCode").value("SLEEP"))
                .andExpect(jsonPath("$[4].templateCode").value("STRESS"));
    }

    @Test
    @DisplayName("MOOD assignment includes options ordered by orderIndex")
    void moodAssignmentIncludesOptions() throws Exception {
        String token = registerUser("alice", "UTC");

        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.templateCode == 'MOOD')].options.length()")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.greaterThan(0))))
                .andExpect(jsonPath("$[?(@.templateCode == 'MOOD')].options[0].value")
                        .value("1"))
                .andExpect(jsonPath("$[?(@.templateCode == 'MOOD')].options[0].label")
                        .value("Rất tệ"))
                .andExpect(jsonPath("$[?(@.templateCode == 'MOOD')].options[0].orderIndex")
                        .value(1));
    }

    // --- Cross-user isolation ---

    @Test
    @DisplayName("Two users on the same day each get their own assignments")
    void crossUser_isolation() throws Exception {
        String aliceToken = registerUser("alice-iso", "UTC");
        String bobToken = registerUser("bob-iso", "UTC");

        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // 10 assignments total in the table — 5 per user
        assertThat(assignmentRepository.count()).isEqualTo(10);
    }

    // --- Timezone resilience ---

    @Test
    @DisplayName("User changes timezone mid-day → does not create a duplicate assignment")
    void userChangesTimezone_sameDate_noDuplicate() throws Exception {
        String token = registerUser("tz-user", "UTC");

        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        // Simulate user changing their timezone
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().startsWith("tz-user-"))
                .findFirst().orElseThrow();
        user.setTimezone("Asia/Ho_Chi_Minh");
        userRepository.save(user);

        // Same calendar day → no new assignments
        mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        assertThat(assignmentRepository.count()).isEqualTo(5);
    }

    // --- Historical consistency ---

    @Test
    @DisplayName("Admin publishing a new template version after assignment does not change the user's already-assigned prompt")
    void templateNewVersion_existingAssignmentStillShowsOldVersion() throws Exception {
        String token = registerUser("hist-user", "UTC");

        MvcResult first = mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String firstMoodPrompt = extractPromptForCode(first, "MOOD");
        assertThat(firstMoodPrompt).isEqualTo("Tâm trạng hôm nay của bạn như thế nào?");

        // Admin publishes a new version of MOOD
        templateService.updateByCode("MOOD",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                        "Tâm trạng hôm nay? (v2 prompt)",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED,
                        null));

        // User's existing assignment must still show the v1 prompt
        MvcResult second = mockMvc.perform(get("/daily-checkins/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String secondMoodPrompt = extractPromptForCode(second, "MOOD");
        assertThat(secondMoodPrompt).isEqualTo("Tâm trạng hôm nay của bạn như thế nào?");
        assertThat(secondMoodPrompt).isNotEqualTo("Tâm trạng hôm nay? (v2 prompt)");
    }

    // --- Authentication ---

    @Test
    @DisplayName("GET /daily-checkins/today without token → 401")
    void noToken_401() throws Exception {
        mockMvc.perform(get("/daily-checkins/today"))
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

    private List<String> extractAssignmentIds(MvcResult result) throws Exception {
        var array = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        List<String> ids = new java.util.ArrayList<>();
        array.forEach(node -> ids.add(node.get("assignmentId").asText()));
        return ids;
    }

    private String extractPromptForCode(MvcResult result, String code) throws Exception {
        var array = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String[] found = {null};
        array.forEach(node -> {
            if (code.equals(node.get("templateCode").asText())) {
                found[0] = node.get("prompt").asText();
            }
        });
        return found[0];
    }

    private void seedMvpTemplates() {
        if (templateRepository.count() > 0) {
            return;
        }
        // 5 MVP templates (approving not required for the service test path;
        // the service uses findLatestApproved which filters by status)
        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "STRESS", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Hôm nay bạn cảm thấy mức stress của mình như thế nào?", null));
        templateService.updateByCode("STRESS",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Hôm nay bạn cảm thấy mức stress của mình như thế nào?",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("STRESS", "1", "5");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "MOOD", com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                "Tâm trạng hôm nay của bạn như thế nào?",
                List.of(
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Rất tệ", 1),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "Tệ", 2),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "Bình thường", 3),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("4", "Tốt", 4),
                        new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("5", "Rất tốt", 5)
                )));
        templateService.updateByCode("MOOD",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SINGLE_CHOICE,
                        "Tâm trạng hôm nay của bạn như thế nào?",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED,
                        List.of(
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("1", "Rất tệ", 1),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("2", "Tệ", 2),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("3", "Bình thường", 3),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("4", "Tốt", 4),
                                new com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest("5", "Rất tốt", 5)
                        )));

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "SLEEP", com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                "Bạn ngủ bao nhiêu giờ đêm qua?", null));
        templateService.updateByCode("SLEEP",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.NUMBER,
                        "Bạn ngủ bao nhiêu giờ đêm qua?",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("SLEEP", "0", "24");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "ENERGY", com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                "Mức năng lượng của bạn hôm nay như thế nào?", null));
        templateService.updateByCode("ENERGY",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.SCALE,
                        "Mức năng lượng của bạn hôm nay như thế nào?",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
        setScaleRange("ENERGY", "1", "5");

        templateService.create(new com.mindbridge.dailyquestion.dto.CreateTemplateRequest(
                "OPEN", com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                "Có điều gì bạn muốn chia sẻ hôm nay không?", null));
        templateService.updateByCode("OPEN",
                new com.mindbridge.dailyquestion.dto.UpdateTemplateRequest(
                        com.mindbridge.dailyquestion.domain.QuestionType.TEXT,
                        "Có điều gì bạn muốn chia sẻ hôm nay không?",
                        com.mindbridge.dailyquestion.domain.TemplateStatus.APPROVED, null));
    }

    /**
     * Helper: set scale range on the latest APPROVED version of a template.
     * Used by tests after the version-pinning flow has run.
     */
    private void setScaleRange(String code, String min, String max) {
        var tpl = templateRepository.findTopByCodeOrderByVersionDesc(code).orElseThrow();
        tpl.setScaleRange(new java.math.BigDecimal(min), new java.math.BigDecimal(max));
        templateRepository.save(tpl);
    }
}
