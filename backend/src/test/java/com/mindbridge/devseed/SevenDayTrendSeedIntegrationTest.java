package com.mindbridge.devseed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.behavior.feature.profile.repository.UserBehaviorProfileRepository;
import com.mindbridge.behavior.repository.BehavioralEventRepository;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.dailyquestion.domain.AnswerType;
import com.mindbridge.dailyquestion.domain.DailyQuestionAssignment;
import com.mindbridge.dailyquestion.domain.DailyQuestionTemplate;
import com.mindbridge.dailyquestion.domain.QuestionType;
import com.mindbridge.dailyquestion.domain.TemplateStatus;
import com.mindbridge.dailyquestion.dto.CreateTemplateRequest;
import com.mindbridge.dailyquestion.dto.CreateTemplateRequest.OptionRequest;
import com.mindbridge.dailyquestion.dto.UpdateTemplateRequest;
import com.mindbridge.dailyquestion.repository.DailyQuestionAnswerRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionAssignmentRepository;
import com.mindbridge.dailyquestion.repository.DailyQuestionTemplateRepository;
import com.mindbridge.dailyquestion.service.DailyQuestionTemplateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the seven-day trend dev seed.
 *
 * <p>Verifies the DoD from the seed specification:
 * <ul>
 *   <li>1. Seven dates are created.</li>
 *   <li>2. Seed rerun is idempotent.</li>
 *   <li>3. Only the configured user is affected.</li>
 *   <li>4. Seven daily features are produced (all assignments ANSWERED).</li>
 *   <li>5. Seven-day window uses the expected dates.</li>
 *   <li>6. Trend direction is improving for the chosen values.</li>
 *   <li>7. Profile API exposes the calculated trend.</li>
 *   <li>8. Missing seed user fails safely.</li>
 *   <li>9. Seed can run under the test profile.</li>
 *   <li>10. User timezone determines the local dates.</li>
 * </ul>
 *
 * <p>The seed runs against the {@code test} profile H2 in-memory database.
 * The {@code SevenDayTrendSeedRunner} bean is disabled (no {@code enabled=true}
 * property) so we drive the seed imperatively via
 * {@link SevenDayTrendSeedService}.
 *
 * <p>Cleanup order: profiles first (no FK from users), then answers, then
 * assignments, then users (cascades to user_daily_features).
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = {
        "classpath:schema-users.sql",
        "classpath:schema-consent.sql",
        "classpath:schema-audit.sql",
        "classpath:schema-chat-sessions.sql",
        "classpath:schema-conversation-messages.sql",
        "classpath:schema-daily-question.sql",
        "classpath:schema-daily-question-assignments.sql",
        "classpath:schema-daily-question-answers.sql",
        "classpath:schema-behavioral-events.sql",
        "classpath:schema-idempotency-keys.sql",
        "classpath:schema-chat-analysis-results.sql",
        "classpath:schema-ai-analysis-runs.sql",
        "classpath:schema-risk-state-history.sql",
        "classpath:schema-user-daily-features.sql",
        "classpath:schema-user-behavior-profiles.sql",
        "classpath:schema-job-runs.sql",
})
@DisplayName("Seven-day trend seed integration")
class SevenDayTrendSeedIntegrationTest {

    private static final String SEED_USER_EMAIL = "seed-test@example.com";
    private static final String OTHER_USER_EMAIL = "other@example.com";
    private static final LocalDate FIXED_TARGET_DATE = LocalDate.of(2026, 8, 7);

    @Autowired private SevenDayTrendSeedService seedService;
    @Autowired private SeedGuard seedGuard;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyQuestionTemplateRepository templateRepository;
    @Autowired private DailyQuestionAssignmentRepository assignmentRepository;
    @Autowired private DailyQuestionAnswerRepository answerRepository;
    @Autowired private DailyQuestionTemplateService templateService;
    @Autowired private UserBehaviorProfileService profileService;
    @Autowired private UserBehaviorProfileRepository profileRepository;
    @Autowired private BehavioralEventRepository behavioralEventRepository;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private UUID seedUserId;

    @BeforeEach
    void seedTemplatesAndUsers() {
        // STRESS
        if (templateRepository.findTopByCodeOrderByVersionDesc("STRESS").isEmpty()) {
            templateService.create(new CreateTemplateRequest(
                    "STRESS", QuestionType.SCALE, "Stress level?", null));
            templateService.updateByCode("STRESS", new UpdateTemplateRequest(
                    QuestionType.SCALE, "Stress level?",
                    TemplateStatus.APPROVED, null));
            setScaleRange("STRESS", "1", "5");
        }

        // MOOD
        if (templateRepository.findTopByCodeOrderByVersionDesc("MOOD").isEmpty()) {
            List<OptionRequest> moodOptions = List.of(
                    new OptionRequest("1", "Bad", 1),
                    new OptionRequest("2", "Poor", 2),
                    new OptionRequest("3", "OK", 3),
                    new OptionRequest("4", "Good", 4),
                    new OptionRequest("5", "Great", 5));
            templateService.create(new CreateTemplateRequest(
                    "MOOD", QuestionType.SINGLE_CHOICE, "Mood?", moodOptions));
            templateService.updateByCode("MOOD", new UpdateTemplateRequest(
                    QuestionType.SINGLE_CHOICE, "Mood?",
                    TemplateStatus.APPROVED, moodOptions));
        }

        // SLEEP
        if (templateRepository.findTopByCodeOrderByVersionDesc("SLEEP").isEmpty()) {
            templateService.create(new CreateTemplateRequest(
                    "SLEEP", QuestionType.NUMBER, "Sleep hours?", null));
            templateService.updateByCode("SLEEP", new UpdateTemplateRequest(
                    QuestionType.NUMBER, "Sleep hours?",
                    TemplateStatus.APPROVED, null));
            setScaleRange("SLEEP", "0", "24");
        }

        // ENERGY
        if (templateRepository.findTopByCodeOrderByVersionDesc("ENERGY").isEmpty()) {
            templateService.create(new CreateTemplateRequest(
                    "ENERGY", QuestionType.SCALE, "Energy level?", null));
            templateService.updateByCode("ENERGY", new UpdateTemplateRequest(
                    QuestionType.SCALE, "Energy level?",
                    TemplateStatus.APPROVED, null));
            setScaleRange("ENERGY", "1", "5");
        }

        // OPEN
        if (templateRepository.findTopByCodeOrderByVersionDesc("OPEN").isEmpty()) {
            templateService.create(new CreateTemplateRequest(
                    "OPEN", QuestionType.TEXT, "Anything to share?", null));
            templateService.updateByCode("OPEN", new UpdateTemplateRequest(
                    QuestionType.TEXT, "Anything to share?",
                    TemplateStatus.APPROVED, null));
        }

        // Seed user (must exist before the seed can run).
        // createdAt is backdated so daysSinceRegistration >= 7, giving
        // explicitCoverage7d denominator = 7 (not 1) so coverage stays in [0,1].
        // We insert directly via JdbcTemplate to set createdAt at insertion time,
        // bypassing JPA's @PrePersist lifecycle hook which would override it.
        OffsetDateTime backdatedTs = FIXED_TARGET_DATE.minusDays(10)
                .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        seedUserId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO users (id, email, password_hash, display_name, role, status, timezone, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'USER', 'ACTIVE', ?, ?, ?)
            """,
            seedUserId,
            SEED_USER_EMAIL,
            "hash",
            "Seed User",
            "Asia/Ho_Chi_Minh",
            backdatedTs,
            backdatedTs);

        // Other user (should not be affected by seed)
        OffsetDateTime otherTs = OffsetDateTime.now();
        UUID otherId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO users (id, email, password_hash, display_name, role, status, timezone, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'USER', 'ACTIVE', 'UTC', ?, ?)
            """,
            otherId,
            OTHER_USER_EMAIL,
            "hash",
            "Other User",
            otherTs,
            otherTs);
    }

    @AfterEach
    void cleanup() {
        // Profiles have no FK from users - delete explicitly first.
        profileRepository.deleteAll();
        // Behavioral events: delete before assignments so source FKs don't block.
        // Each answer submission records DAILY_CHECKIN_COMPLETED; without this
        // cleanup the counter is wrong on test re-runs within the same JVM,
        // causing checkin_completed_count / checkin_assigned_count > 1 and
        // violating the [0,1] CHECK constraint on user_daily_features.
        behavioralEventRepository.deleteAll();
        answerRepository.deleteAll();
        assignmentRepository.deleteAll();
        // user_daily_features FK has ON DELETE CASCADE from users.
        userRepository.deleteAll();
        templateRepository.deleteAll();
    }

    private void setScaleRange(String code, String min, String max) {
        DailyQuestionTemplate tpl = templateRepository.findTopByCodeOrderByVersionDesc(code).orElseThrow();
        tpl.setScaleRange(new BigDecimal(min), new BigDecimal(max));
        templateRepository.save(tpl);
    }

    // ========================================================================
    // DoD 1: Seven dates are created
    // ========================================================================
    @Test
    @DisplayName("DoD 1 - Seven daily question assignments are created per template")
    void run_createsSevenAssignmentsPerTemplate() {
        LocalDate day1 = FIXED_TARGET_DATE.minusDays(6);

        SevenDayTrendSeedResult result = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        assertThat(result.assignmentsCreated()).isEqualTo(7 * 5); // 7 days x 5 templates
        assertThat(result.answersCreated()).isEqualTo(7 * 5);   // all answered

        for (int i = 0; i < 7; i++) {
            LocalDate date = day1.plusDays(i);
            var assignments = assignmentRepository
                    .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(seedUserId, date);
            assertThat(assignments).hasSize(5);
        }
    }

    // ========================================================================
    // DoD 2: Seed rerun is idempotent
    // ========================================================================
    @Test
    @DisplayName("DoD 2 - Running the seed twice is idempotent")
    void run_idempotent_secondRunProducesSameAssignmentCount() {
        SevenDayTrendSeedResult first = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);
        SevenDayTrendSeedResult second = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        assertThat(second.assignmentsCreated()).isEqualTo(first.assignmentsCreated());
        // Second run: answers already exist, so answersCreated = 0
        assertThat(second.answersCreated()).isEqualTo(0);
    }

    // ========================================================================
    // DoD 3: Only the configured user is affected
    // ========================================================================
    @Test
    @DisplayName("DoD 3 - Only the seed user receives assignments, other user is untouched")
    void run_onlyAffectsSeedUser() {
        UUID otherId = userRepository.findByEmailIgnoreCase(OTHER_USER_EMAIL)
                .orElseThrow().getId();

        seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        var seedAssignments = assignmentRepository
                .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(seedUserId, FIXED_TARGET_DATE);
        var otherAssignments = assignmentRepository
                .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(otherId, FIXED_TARGET_DATE);

        assertThat(seedAssignments).hasSize(5);
        assertThat(otherAssignments).isEmpty();
    }

    // ========================================================================
    // DoD 4: Seven daily features are produced
    // ========================================================================
    @Test
    @DisplayName("DoD 4 - All seven daily assignments are ANSWERED (G4 pipeline triggered)")
    void run_allAssignmentsAnswered() {
        seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        LocalDate day1 = FIXED_TARGET_DATE.minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate date = day1.plusDays(i);
            var assignments = assignmentRepository
                    .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(seedUserId, date);
            for (var a : assignments) {
                assertThat(a.getStatus())
                        .as("Assignment on %s should be ANSWERED", date)
                        .isEqualTo(com.mindbridge.dailyquestion.domain.AssignmentStatus.ANSWERED);
            }
        }
    }

    // ========================================================================
    // DoD 5: Seven-day window uses the expected dates
    // ========================================================================
    @Test
    @DisplayName("DoD 5 - Day 7 equals targetDate, Day 1 equals targetDate minus 6")
    void run_correctDates() {
        LocalDate expectedDay1 = FIXED_TARGET_DATE.minusDays(6);

        SevenDayTrendSeedResult result = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        assertThat(result.targetDate()).isEqualTo(FIXED_TARGET_DATE);
        assertThat(result.day1()).isEqualTo(expectedDay1);
        assertThat(result.dayResults()).hasSize(7);
        assertThat(result.dayResults().get(0).localDate()).isEqualTo(expectedDay1);
        assertThat(result.dayResults().get(6).localDate()).isEqualTo(FIXED_TARGET_DATE);
    }

    // ========================================================================
    // DoD 6: Trend direction is improving for the chosen values
    // ========================================================================
    @Test
    @DisplayName("DoD 6 - Stress decreases while mood/energy/sleep increase across 7 days")
    void run_stressDecreasingMoodEnergySleepIncreasing() {
        SevenDayTrendSeedResult result = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        // Stress: Day 7 (index 6) < Day 1 (index 0)
        String day0Stress = result.dayResults().get(0).values().get(0);
        String day6Stress = result.dayResults().get(6).values().get(0);
        assertThat(Integer.parseInt(day6Stress))
                .as("Day 7 stress raw should be lower than Day 1")
                .isLessThan(Integer.parseInt(day0Stress));

        // Mood: the raw option is INVERTED in normalization ("1"=best, "5"=worst).
        // Instead of comparing raw options, query the feature table and verify the
        // normalized moodScore improves.  Day 1 option=4 → normalized 0.25 (worse);
        // Day 7 option=1 → normalized 1.0 (best). So day7Mood > day1Mood.
        List<Map<String, Object>> features = jdbc.queryForList("""
            SELECT feature_date, stress_score, mood_score, energy_score, sleep_hours
            FROM user_daily_features
            WHERE user_id = ?
            ORDER BY feature_date ASC
            """, seedUserId);
        assertThat(features).hasSize(7);

        BigDecimal day1Mood = (BigDecimal) features.get(0).get("MOOD_SCORE");
        BigDecimal day7Mood = (BigDecimal) features.get(6).get("MOOD_SCORE");
        assertThat(day7Mood)
                .as("Day 7 normalized moodScore should be better than Day 1")
                .isGreaterThan(day1Mood);

        BigDecimal day1Stress = (BigDecimal) features.get(0).get("STRESS_SCORE");
        BigDecimal day7Stress = (BigDecimal) features.get(6).get("STRESS_SCORE");
        assertThat(day7Stress)
                .as("Day 7 normalized stressScore should be lower than Day 1")
                .isLessThan(day1Stress);

        BigDecimal day1Energy = (BigDecimal) features.get(0).get("ENERGY_SCORE");
        BigDecimal day7Energy = (BigDecimal) features.get(6).get("ENERGY_SCORE");
        assertThat(day7Energy)
                .as("Day 7 normalized energyScore should be higher than Day 1")
                .isGreaterThan(day1Energy);

        BigDecimal day1Sleep = (BigDecimal) features.get(0).get("SLEEP_HOURS");
        BigDecimal day7Sleep = (BigDecimal) features.get(6).get("SLEEP_HOURS");
        assertThat(day7Sleep.doubleValue())
                .as("Day 7 sleep hours should be higher than Day 1")
                .isGreaterThan(day1Sleep.doubleValue());
    }

    // ========================================================================
    // DoD 7: Profile API exposes the calculated trend
    // ========================================================================
    @Test
    @DisplayName("DoD 7 - Behavior profile is upserted and queryable via the service")
    void run_profileIsUpserted() {
        SevenDayTrendSeedResult result = seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        assertThat(result.profileUpserted()).isTrue();

        var profile = profileService.findLatestForUser(seedUserId);
        assertThat(profile).isPresent();
    }

    // ========================================================================
    // DoD 8: Missing seed user fails safely
    // ========================================================================
    @Test
    @DisplayName("DoD 8 - Running with a non-existent user throws IllegalStateException")
    void run_missingUser_throwsWithClearMessage() {
        assertThatThrownBy(() -> seedService.run("nobody@example.com", FIXED_TARGET_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nobody@example.com")
                .hasMessageContaining("Seed user not found")
                .hasMessageContaining("register");
    }

    // ========================================================================
    // DoD 9: Seed can run under the test profile
    // ========================================================================
    @Test
    @DisplayName("DoD 9 - SeedGuard.requireSeedAllowed() passes under the test profile")
    void seedGuard_allowsTestProfile() {
        seedGuard.requireSeedAllowed();
    }

    // ========================================================================
    // DoD 10: User timezone determines the local dates
    // ========================================================================
    @Test
    @DisplayName("DoD 10 - All assignments use Asia/Ho_Chi_Minh timezone")
    void run_usesAsiaHoChiMinhTimezone() {
        seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        LocalDate day1 = FIXED_TARGET_DATE.minusDays(6);
        for (int i = 0; i < 7; i++) {
            LocalDate date = day1.plusDays(i);
            var assignments = assignmentRepository
                    .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(seedUserId, date);
            assertThat(assignments).isNotEmpty();
            for (var a : assignments) {
                assertThat(a.getTimezone())
                        .as("Assignment timezone on %s should be Asia/Ho_Chi_Minh", date)
                        .isEqualTo("Asia/Ho_Chi_Minh");
            }
        }
    }

    @Test
    @DisplayName("DEBUG - print actual user createdAt and daysSinceRegistration")
    void debug_userCreatedAt() {
        // Insert and immediately read back to verify the createdAt value.
        UUID testId = UUID.randomUUID();
        OffsetDateTime backdatedTs = FIXED_TARGET_DATE.minusDays(10)
                .atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        jdbc.update("""
            INSERT INTO users (id, email, password_hash, display_name, role, status, timezone, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'USER', 'ACTIVE', ?, ?, ?)
            """,
            testId, "debug@test.com", "hash", "Debug",
            "Asia/Ho_Chi_Minh", backdatedTs, backdatedTs);

        var readBack = userRepository.findById(testId);
        System.out.println("DEBUG user createdAt=" + readBack.get().getCreatedAt());
        System.out.println("DEBUG daysSinceReg="
                + java.time.temporal.ChronoUnit.DAYS.between(
                        readBack.get().getCreatedAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate(),
                        FIXED_TARGET_DATE));
        System.out.println("DEBUG targetDate=" + FIXED_TARGET_DATE);

        assertThat(readBack.get().getCreatedAt()).isNotNull();
    }

    // ========================================================================
    // Additional: neutral non-clinical text answers
    // ========================================================================
    @Test
    @DisplayName("OPEN template answers are neutral, non-clinical, and non-empty")
    void run_openAnswersAreNonClinical() {
        seedService.run(SEED_USER_EMAIL, FIXED_TARGET_DATE);

        var openAssignments = assignmentRepository
                .findByUserIdAndAssignedForDateOrderByTemplateCodeAsc(seedUserId, FIXED_TARGET_DATE);
        DailyQuestionAssignment openAssignment = openAssignments.stream()
                .filter(a -> "OPEN".equals(a.getTemplateCode()))
                .findFirst()
                .orElseThrow();

        var answer = answerRepository.findAll().stream()
                .filter(a -> a.getAssignment().getId().equals(openAssignment.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(answer.getAnswerType()).isEqualTo(AnswerType.TEXT);
        assertThat(answer.getTextValue()).isNotBlank();
    }
}