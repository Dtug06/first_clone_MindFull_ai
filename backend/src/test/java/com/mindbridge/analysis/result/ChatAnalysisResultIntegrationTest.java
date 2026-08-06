package com.mindbridge.analysis.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.analysis.result.dto.ChatAnalysisResultSummary;
import com.mindbridge.analysis.result.service.ChatAnalysisResultService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.analysis-run.provider-label=mock",
        "mindbridge.ai.analysis-run.mock-model=MOCK_V1",
        "mindbridge.ai.analysis-run.prompt-version="
})
@Sql(scripts = {
        "/schema-users.sql",
        "/schema-chat-sessions.sql",
        "/schema-conversation-messages.sql",
        "/schema-ai-analysis-runs.sql",
        "/schema-chat-analysis-results.sql"
})
@DisplayName("ChatAnalysisResultService integration")
class ChatAnalysisResultIntegrationTest {

    @Autowired
    private ChatAnalysisResultService service;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID sessionId;
    private UUID messageId;
    private UUID userId;

    @BeforeEach
    void insertMessageParent() {
        sessionId = UUID.randomUUID();
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();

        jdbc.update(
                "INSERT INTO chat_sessions (id, user_id, created_at, updated_at) VALUES (?, ?, ?, ?)",
                sessionId, userId, OffsetDateTime.now(), OffsetDateTime.now());

        jdbc.update(
                "INSERT INTO conversation_messages (id, session_id, user_id, role, content, redacted, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                messageId, sessionId, userId, "USER",
                "Feeling tired with work today", false,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Nested
    class BasicRecording {

        @Test
        @DisplayName("records analysis result successfully")
        void recordResult_success() {
            UUID runId = insertRun(messageId, userId);
            ChatAnalysisResultSummary summary = service.recordResult(runId, okOutput());

            assertThat(summary.analysisStatus()).isEqualTo(ResultAnalysisStatus.ACTIVE);
            assertThat(summary.supersedesId()).isNull();
            assertThat(summary.modelRiskLevel()).isEqualTo((short) 1);
            assertThat(summary.confidence()).isEqualByComparingTo("0.72");
        }

        @Test
        @DisplayName("stores topic/emotion/intent as strings")
        void storesEnumStrings() {
            UUID runId = insertRun(messageId, userId);
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.RELATIONSHIP, Emotion.SAD, Intent.ADVICE,
                    List.of(Signal.BURNOUT), 2, 0.88,
                    List.of(), 20L, null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

            ChatAnalysisResultSummary summary = service.recordResult(runId, output);

            assertThat(summary.topic()).isEqualTo("RELATIONSHIP");
            assertThat(summary.emotion()).isEqualTo("SAD");
            assertThat(summary.intent()).isEqualTo("ADVICE");
        }

        @Test
        @DisplayName("stores multiple signals")
        void storesMultipleSignals() {
            UUID runId = insertRun(messageId, userId);
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.WORK_STRESS, Emotion.DISTRESS, Intent.VENT,
                    List.of(Signal.BURNOUT, Signal.ISOLATION, Signal.HOPELESSNESS),
                    3, 0.91,
                    List.of(), 25L, null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

            ChatAnalysisResultSummary summary = service.recordResult(runId, output);

            assertThat(summary.signals())
                    .containsExactlyInAnyOrder("BURNOUT", "ISOLATION", "HOPELESSNESS");
        }

        @Test
        @DisplayName("stores risk level 4")
        void storesRiskLevel4() {
            UUID runId = insertRun(messageId, userId);
            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.HEALTH, Emotion.DISTRESS, Intent.SUPPORT,
                    List.of(Signal.SELF_HARM_RISK), 4, 0.99,
                    List.of(), 5L, null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

            ChatAnalysisResultSummary summary = service.recordResult(runId, output);

            assertThat(summary.modelRiskLevel()).isEqualTo((short) 4);
        }
    }

    @Nested
    class EffectiveResult {

        @Test
        @DisplayName("getEffectiveResult returns recorded result")
        void getEffectiveResult_returnsResult() {
            UUID runId = insertRun(messageId, userId);
            service.recordResult(runId, okOutput());

            var result = service.getEffectiveResult(messageId);
            assertThat(result).isPresent();
            assertThat(result.get().analysisStatus()).isEqualTo(ResultAnalysisStatus.ACTIVE);
        }

        @Test
        @DisplayName("getEffectiveResult returns empty when no analysis")
        void getEffectiveResult_emptyWhenNoAnalysis() {
            var result = service.getEffectiveResult(messageId);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class MessageImmutability {

        @Test
        @DisplayName("conversation_messages row unchanged after analysis")
        void messageUnchanged_afterAnalysis() {
            String contentBefore = jdbc.queryForObject(
                    "SELECT content FROM conversation_messages WHERE id = ?",
                    String.class, messageId);

            UUID runId = insertRun(messageId, userId);
            service.recordResult(runId, okOutput());

            String contentAfter = jdbc.queryForObject(
                    "SELECT content FROM conversation_messages WHERE id = ?",
                    String.class, messageId);

            assertThat(contentAfter).isEqualTo(contentBefore);
        }
    }

    private UUID insertRun(UUID messageId, UUID userId) {
        UUID runId = UUID.randomUUID();
        String inputHash = "a".repeat(64);
        OffsetDateTime now = OffsetDateTime.now();

        jdbc.update(
                "INSERT INTO ai_analysis_runs "
                        + "(id, message_id, user_id, provider, model, prompt_version, schema_version, "
                        + "status, input_hash, output_hash, error_code, error_summary, "
                        + "latency_ms, input_tokens, output_tokens, model_risk_level, confidence, "
                        + "created_at, started_at, completed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                runId, messageId, userId,
                "mock", "MOCK_V1", "v1:5363675e22fe", "V1",
                "SUCCEEDED", inputHash, "b".repeat(64), null, null,
                15, null, null, (short) 1, "0.72",
                now, now, now);
        return runId;
    }

    private static ChatAnalysisOutput okOutput() {
        return new ChatAnalysisOutput(
                Topic.WORK_STRESS, Emotion.NEUTRAL, Intent.VENT,
                List.of(), 1, 0.72,
                List.of(), 15L, null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
    }
}