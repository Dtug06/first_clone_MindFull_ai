package com.mindbridge.analysis.run;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.RealLlmResponseContext;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import com.mindbridge.analysis.run.domain.AiAnalysisRunService;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import com.mindbridge.analysis.run.dto.AiRunSummary;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test for the G3-T06 end-to-end claim: when the
 * {@code ChatAnalysisProvider} populates {@link RealLlmResponseContext}
 * (i.e. when it's the real LLM provider), {@code AiAnalysisRunService}
 * persists the {@code provider} and {@code model} columns from the
 * snapshot — NOT the constructor-injected placeholder labels.
 *
 * <p>The real provider itself is replaced here with a tiny fake that
 * simulates the real flow (real provider → snapshot → service override),
 * so this test boots no real network and depends only on the H2
 * schema mirror. No need for an active API key.
 */
@SpringBootTest
@Import(AiAnalysisRunServiceRealProviderIntegrationTest.FakeRealProviderConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",   // config picks the mock bean...
        // ...but the @Primary ChatAnalysisProvider below replaces it
        // with our fake-real, which populates the response context.
        "mindbridge.ai.analysis-run.provider-label=placeholder",
        "mindbridge.ai.analysis-run.mock-model=PLACEHOLDER",
        "mindbridge.ai.analysis-run.prompt-version=v1:5363675e22fe"
})
@Sql(scripts = {
        "/schema-conversation-messages.sql",
        "/schema-ai-analysis-runs.sql"
})
@DisplayName("AiAnalysisRunService — real-provider path")
class AiAnalysisRunServiceRealProviderIntegrationTest {

    @Autowired
    private AiAnalysisRunService service;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID messageId;
    private UUID userId;

    @BeforeEach
    void insertMessageParent() {
        messageId = UUID.randomUUID();
        userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO conversation_messages (id, session_id, user_id, role, content, redacted, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                messageId.toString(), sessionId.toString(), userId.toString(),
                "USER", "fake-real integration content", false);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM ai_analysis_runs WHERE message_id = ?", messageId.toString());
        jdbc.update("DELETE FROM conversation_messages WHERE id = ?", messageId.toString());
        RealLlmResponseContext.clear();
    }

    @Test
    @DisplayName("real provider's snapshot values land in ai_analysis_runs.provider and .model")
    void realProvider_overridesProviderAndModel() {
        ChatAnalysisInput input = new ChatAnalysisInput(
                messageId, userId,
                "real provider e2e", "vi-VN");

        AiRunSummary summary = service.startRun(input);

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.SUCCEEDED);
        // The fake populates the snapshot with "openai" / "gpt-4o-mini-2024-07-18".
        assertThat(summary.provider()).isEqualTo("openai");
        assertThat(summary.model()).isEqualTo("gpt-4o-mini-2024-07-18");

        // Verify the row in H2 reflects the same (no JPA layer rewriting).
        String rowProvider = jdbc.queryForObject(
                "SELECT provider FROM ai_analysis_runs WHERE id = ?",
                String.class, summary.id().toString());
        String rowModel = jdbc.queryForObject(
                "SELECT model FROM ai_analysis_runs WHERE id = ?",
                String.class, summary.id().toString());
        assertThat(rowProvider).isEqualTo("openai");
        assertThat(rowModel).isEqualTo("gpt-4o-mini-2024-07-18");
    }

    /**
     * Replaces the {@code ChatAnalysisProvider} bean for THIS test
     * class only. The fake simulates the real provider by populating
     * {@link RealLlmResponseContext} before returning the output,
     * exactly as {@code RealLlmChatAnalysisProvider} does on a
     * successful 200 response (per G3-T06 §6 §3 "Ghi lại model
     * name/revision THỰC TẾ").
     */
    @TestConfiguration
    static class FakeRealProviderConfig {

        @Bean
        @Primary
        ChatAnalysisProvider fakeRealProvider() {
            return new ChatAnalysisProvider() {
                @Override
                public ChatAnalysisOutput analyze(ChatAnalysisInput input) {
                    RealLlmResponseContext.set(
                            new RealLlmResponseContext.Snapshot(
                                    "openai",
                                    "gpt-4o-mini-2024-07-18"));
                    return new ChatAnalysisOutput(
                            Topic.WORK_STRESS, Emotion.ANXIOUS, Intent.VENT,
                            List.of(Signal.BURNOUT),
                            2, 0.78,
                            List.of(),
                            42L,
                            null,
                            AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
                }
            };
        }
    }
}
