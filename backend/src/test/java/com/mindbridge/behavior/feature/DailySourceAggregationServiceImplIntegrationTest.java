package com.mindbridge.behavior.feature;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.behavior.feature.dto.CbtAvailability;
import com.mindbridge.behavior.feature.dto.DailySourceAggregation;
import com.mindbridge.behavior.feature.impl.DailySourceAggregationServiceImpl;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

/**
 * G4-T03: Integration test for {@link DailySourceAggregationServiceImpl}.
 *
 * <p>Boots the full Spring context against the in-memory H2 mirror, then
 * seeds raw source rows via {@link JdbcTemplate} so the service reads from
 * real JPA-managed entities (not mocks).
 *
 * <p>Covers the 4 Definition-of-Done scenarios:
 * <ol>
 *   <li>Happy path: a user with explicit answers + ACTIVE chat rows +
 *       behavioral events on a local day returns them all correctly aggregated.</li>
 *   <li>Rerun-aware: a SUPERSEDED chat row is NOT included.</li>
 *   <li>Late-arriving answer: an answer whose {@code answered_at} falls AFTER
 *       the local-day window but whose assignment's
 *       {@code assigned_for_date} matches IS included.</li>
 *   <li>Cross-user isolation: another user's data on the same day is NOT
 *       included in this user's aggregation.</li>
 * </ol>
 *
 * <p>Timezone: tests use Asia/Ho_Chi_Minh (UTC+7, no DST) so the local-date
 * window math is unambiguous across test runs.
 */
@SpringBootTest
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
        "classpath:schema-chat-analysis-results.sql",
        "classpath:schema-idempotency-keys.sql"
})
@DisplayName("DailySourceAggregationService integration")
class DailySourceAggregationServiceImplIntegrationTest {

    private static final String TZ = "Asia/Ho_Chi_Minh";
    private static final LocalDate DAY = LocalDate.of(2026, 8, 4);

    @Autowired
    private DailySourceAggregationServiceImpl service;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID userA;
    private UUID userB;
    private UUID templateId;
    private UUID assignmentA_mood;
    private UUID assignmentA_sleep;
    private UUID assignmentA_stress;
    private UUID assignmentB_mood;
    private UUID sessionId;
    private UUID messageId;
    private UUID runId;

    @BeforeEach
    void seedFixtures() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
        insertUser(userA, "userA-" + userA + "@test.local");
        insertUser(userB, "userB-" + userB + "@test.local");

        // Daily question templates - one per template_code we use.
        // The H2 mirror has NO FK to daily_question_templates so we could
        // get away with random UUIDs, but DailyQuestionAssignment.templateVersion
        // is a LAZY @ManyToOne and ExplicitAnswer.of(...) reads
        // assignment.getTemplateVersion().getQuestionType() — so the template
        // rows MUST exist for every assignment.
        templateId = insertTemplate("MOOD_SCALE");
        UUID sleepId  = insertTemplate("SLEEP_SCALE");
        UUID stressId = insertTemplate("STRESS_SCALE");
        UUID lateId   = insertTemplate("LATE_ANSWER");

        // Assignments for userA on DAY: three on DAY (mood/sleep/stress) plus
        // one for userB. The late-arriving-answer test inserts a 4th for userA
        // using lateId. Stash the IDs we need to reference after @BeforeEach.
        assignmentA_mood   = insertAssignment(userA, "MOOD_SCALE",   DAY, templateId);
        assignmentA_sleep  = insertAssignment(userA, "SLEEP_SCALE",  DAY, sleepId);
        assignmentA_stress = insertAssignment(userA, "STRESS_SCALE", DAY, stressId);
        assignmentB_mood   = insertAssignment(userB, "MOOD_SCALE",   DAY, templateId);
        // userB needs no other assignments; we only assert isolation on answers.

        // Answers for userA: mood = 7, sleep = 6, stress = 8 (all NUMERIC)
        insertNumericAnswer(assignmentA_mood,   userA, new java.math.BigDecimal("7"));
        insertNumericAnswer(assignmentA_sleep,  userA, new java.math.BigDecimal("6"));
        insertNumericAnswer(assignmentA_stress, userA, new java.math.BigDecimal("8"));
        // userB answers for isolation check
        insertNumericAnswer(assignmentB_mood,   userB, new java.math.BigDecimal("3"));

        // Behavioral events for userA within DAY (UTC window = DAY 00:00 +07:00 = DAY-1 17:00 UTC .. DAY 16:59 UTC)
        Instant dayStartUtc = DAY.atStartOfDay(ZoneId.of(TZ)).toInstant();
        insertBehavioralEvent(userA, com.mindbridge.behavior.domain.BehavioralEventType.CHAT_MESSAGE_SENT,
                com.mindbridge.behavior.domain.SourceType.CONVERSATION_MESSAGE, UUID.randomUUID(),
                dayStartUtc.plusSeconds(3600));     // +1h
        insertBehavioralEvent(userA, com.mindbridge.behavior.domain.BehavioralEventType.CHAT_MESSAGE_SENT,
                com.mindbridge.behavior.domain.SourceType.CONVERSATION_MESSAGE, UUID.randomUUID(),
                dayStartUtc.plusSeconds(7200));     // +2h
        insertBehavioralEvent(userA, com.mindbridge.behavior.domain.BehavioralEventType.CHAT_SESSION_STARTED,
                com.mindbridge.behavior.domain.SourceType.CHAT_SESSION, UUID.randomUUID(),
                dayStartUtc.plusSeconds(60));
        insertBehavioralEvent(userA, com.mindbridge.behavior.domain.BehavioralEventType.DAILY_CHECKIN_COMPLETED,
                com.mindbridge.behavior.domain.SourceType.DAILY_QUESTION_ASSIGNMENT, assignmentA_mood,
                dayStartUtc.plusSeconds(1800));
        insertBehavioralEvent(userA, com.mindbridge.behavior.domain.BehavioralEventType.DAILY_CHECKIN_SKIPPED,
                com.mindbridge.behavior.domain.SourceType.DAILY_QUESTION_ASSIGNMENT, assignmentA_sleep,
                dayStartUtc.plusSeconds(1800));

        // Chat analysis: one ACTIVE row in window for userA, one SUPERSEDED in window, one OUTSIDE window
        sessionId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        runId = UUID.randomUUID();
        insertChatSession(sessionId, userA);
        insertConversationMessage(messageId, sessionId, userA);

        // ACTIVE row inside window
        UUID activeRow = UUID.randomUUID();
        insertChatAnalysisResult(activeRow, runId, messageId, userA,
                ResultAnalysisStatus.ACTIVE,
                DAY.atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime().plusHours(5));
        // SUPERSEDED row inside window (should NOT be returned)
        UUID supersededRow = UUID.randomUUID();
        insertChatAnalysisResult(supersededRow, runId, messageId, userA,
                ResultAnalysisStatus.SUPERSEDED,
                DAY.atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime().plusHours(3));
        // ACTIVE row outside window (previous day) - should NOT be returned
        UUID prevDayRow = UUID.randomUUID();
        insertChatAnalysisResult(prevDayRow, runId, messageId, userA,
                ResultAnalysisStatus.ACTIVE,
                DAY.atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime().minusHours(5));
    }

    @AfterEach
    void cleanup() {
        // H2 mirror persists across tests (DB_CLOSE_DELAY=-1). Wipe only the
        // tables this test writes to so the unique-email constraint and any
        // dangling FK-target rows do not leak into the next test.
        jdbc.update("DELETE FROM chat_analysis_results");
        jdbc.update("DELETE FROM behavioral_events");
        jdbc.update("DELETE FROM daily_question_answers");
        jdbc.update("DELETE FROM daily_question_assignments");
        jdbc.update("DELETE FROM daily_question_templates");
        jdbc.update("DELETE FROM conversation_messages");
        jdbc.update("DELETE FROM chat_sessions");
        jdbc.update("DELETE FROM users");
    }

    private void insertUser(UUID id, String email) {
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, timezone, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, email, "hash", "Test User", "USER", TZ, "ACTIVE",
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private UUID insertTemplate(String code) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO daily_question_templates "
                + "(id, code, version, question_type, prompt, status, scale_min, scale_max, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, code, 1, "SCALE", "How is your " + code + "?", "APPROVED",
                new java.math.BigDecimal("0"), new java.math.BigDecimal("10"),
                OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    private UUID insertAssignment(UUID userId, String templateCode, LocalDate forDate, UUID templateVersionId) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO daily_question_assignments "
                + "(id, user_id, template_version_id, template_code, assigned_for_date, timezone, status, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, userId, templateVersionId, templateCode, forDate, TZ, "ANSWERED",
                OffsetDateTime.now(), OffsetDateTime.now());
        return id;
    }

    private void insertNumericAnswer(UUID assignmentId, UUID userId, java.math.BigDecimal value) {
        jdbc.update("INSERT INTO daily_question_answers "
                + "(id, assignment_id, user_id, answer_type, numeric_value, answered_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), assignmentId, userId, "NUMERIC", value, OffsetDateTime.now());
    }

    private void insertBehavioralEvent(UUID userId,
                                       com.mindbridge.behavior.domain.BehavioralEventType type,
                                       com.mindbridge.behavior.domain.SourceType sourceType,
                                       UUID sourceId,
                                       Instant occurredAt) {
        jdbc.update("INSERT INTO behavioral_events "
                + "(id, user_id, event_type, source_type, source_id, occurred_at, local_date, timezone, schema_version) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), userId, type.name(), sourceType.name(), sourceId,
                OffsetDateTime.ofInstant(occurredAt, ZoneId.of("UTC")),
                occurredAt.atZone(ZoneId.of(TZ)).toLocalDate(), TZ, (short) 1);
    }

    private void insertChatSession(UUID id, UUID userId) {
        jdbc.update("INSERT INTO chat_sessions (id, user_id, created_at, updated_at) VALUES (?, ?, ?, ?)",
                id, userId, OffsetDateTime.now(), OffsetDateTime.now());
    }

    private void insertConversationMessage(UUID id, UUID sessionId, UUID userId) {
        jdbc.update("INSERT INTO conversation_messages "
                + "(id, session_id, user_id, role, content, redacted, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, sessionId, userId, "USER", "test message", false,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    private void insertChatAnalysisResult(UUID id, UUID runId, UUID msgId, UUID userId,
                                          ResultAnalysisStatus status, OffsetDateTime createdAt) {
        jdbc.update("INSERT INTO chat_analysis_results "
                + "(id, analysis_run_id, conversation_message_id, user_id, topic, emotion, intent, "
                + " signals, evidence_spans, model_risk_level, confidence, analysis_status, supersedes_id, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, runId, msgId, userId,
                "WORK_STRESS", "NEUTRAL", "VENT",
                "[]", "[]", 1, new java.math.BigDecimal("0.70"),
                status.name(), null, createdAt);
    }

    // ------------------------------------------------------------------
    // Definition of Done scenarios
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DoD-1: Happy path - aggregates 3 explicit answers, 1 ACTIVE chat, 4 event types")
    void happyPath_aggregatesAllSources() {
        DailySourceAggregation out = service.aggregateForDay(userA, TZ, DAY);

        assertThat(out.userId()).isEqualTo(userA);
        assertThat(out.timezone()).isEqualTo(TZ);
        assertThat(out.localDate()).isEqualTo(DAY);
        assertThat(out.windowStartUtc()).isEqualTo(DAY.atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime());
        assertThat(out.windowEndUtc()).isEqualTo(DAY.plusDays(1).atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime());

        assertThat(out.explicitAnswers()).hasSize(3);
        assertThat(out.effectiveChatAnalyses()).hasSize(1);

        assertThat(out.behavioralCounts().chatMessageCount()).isEqualTo(2L);
        assertThat(out.behavioralCounts().activeChatSessionCount()).isEqualTo(1L);
        assertThat(out.behavioralCounts().checkinCompletedCount()).isEqualTo(1L);
        assertThat(out.behavioralCounts().checkinSkippedCount()).isEqualTo(1L);

        assertThat(out.cbtAvailability()).isEqualTo(CbtAvailability.NOT_SHIPPED);
    }

    @Test
    @DisplayName("DoD-2: Rerun-aware - SUPERSEDED chat row is excluded; OUTSIDE-window row is excluded")
    void rerunAware_supersededAndOutsideWindow_excluded() {
        DailySourceAggregation out = service.aggregateForDay(userA, TZ, DAY);

        // Only the one in-window ACTIVE row should appear.
        assertThat(out.effectiveChatAnalyses()).hasSize(1);
        // The included row's createdAt is in the local-day window
        assertThat(out.effectiveChatAnalyses().get(0).createdAt())
                .isAfterOrEqualTo(out.windowStartUtc().toInstant())
                .isBefore(out.windowEndUtc().toInstant());
    }

    @Test
    @DisplayName("DoD-3: Late-arriving answer - answered_at outside window but assignedForDate matches -> included")
    void lateArrivingAnswer_included() {
        // The seedFixtures() already created a LATE_ANSWER template for us.
        // Just insert one extra assignment + one answer whose answered_at falls
        // AFTER the local-day window but whose assignedForDate = DAY.
        // Under Q1=A policy this answer is still attributed to DAY.
        UUID lateTplId = insertTemplate("LATE_ANSWER_V2");  // unique to avoid UNIQUE(code,version)
        UUID lateAssignment = insertAssignment(userA, "LATE_ANSWER_V2", DAY, lateTplId);
        OffsetDateTime outsideWindow = DAY.plusDays(1).atStartOfDay(ZoneId.of(TZ)).toOffsetDateTime().plusHours(2);
        jdbc.update("INSERT INTO daily_question_answers "
                + "(id, assignment_id, user_id, answer_type, numeric_value, answered_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), lateAssignment, userA, "NUMERIC", new java.math.BigDecimal("5"), outsideWindow);

        DailySourceAggregation out = service.aggregateForDay(userA, TZ, DAY);

        // seedFixtures inserted 3 answers; this test adds 1 -> 4.
        assertThat(out.explicitAnswers()).hasSize(4);
        assertThat(out.explicitAnswers())
                .extracting(a -> a.assignedForDate())
                .containsOnly(DAY);
    }

    @Test
    @DisplayName("DoD-4: Cross-user isolation - userB's data on DAY is NOT in userA's aggregation")
    void crossUserIsolation() {
        DailySourceAggregation out = service.aggregateForDay(userA, TZ, DAY);

        // userB has 1 answer and 0 events (we did not seed any).
        // userA must have only their own 3 answers and the seeded events.
        assertThat(out.explicitAnswers())
                .allSatisfy(a -> assertThat(a.assignmentId()).isNotEqualTo(assignmentB_mood));

        // Insert an event for userB inside window - must NOT appear in userA's aggregation.
        insertBehavioralEvent(userB, com.mindbridge.behavior.domain.BehavioralEventType.CHAT_MESSAGE_SENT,
                com.mindbridge.behavior.domain.SourceType.CONVERSATION_MESSAGE, UUID.randomUUID(),
                DAY.atStartOfDay(ZoneId.of(TZ)).toInstant().plusSeconds(1800));

        DailySourceAggregation out2 = service.aggregateForDay(userA, TZ, DAY);
        assertThat(out2.behavioralCounts().chatMessageCount()).isEqualTo(2L);
    }
}
