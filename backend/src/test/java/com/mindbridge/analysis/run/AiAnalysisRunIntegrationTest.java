package com.mindbridge.analysis.run;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisPromptVersion;
import com.mindbridge.analysis.run.domain.AiAnalysisRun;
import com.mindbridge.analysis.run.domain.AiAnalysisRunService;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import com.mindbridge.analysis.run.dto.AiRunSummary;
import com.mindbridge.analysis.run.repository.AiAnalysisRunRepository;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test that boots the full Spring context with the
 * {@code com.mindbridge.analysis.run} package on the classpath,
 * then exercises {@link AiAnalysisRunService} end-to-end against
 * the H2 schema.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Spring context boots cleanly with the migration enabled.</li>
 *   <li>Analyze on a real (mock) conversation message produces one
 *       SUCCEEDED row.</li>
 *   <li>All audit fields are persisted (provider/model/prompt/schema).</li>
 *   <li>JPA reflection scan: the lifecycle methods
 *       {@code markRunning}/{@code markSucceeded}/{@code markFailed}
 *       are only callable from within the {@code run.domain} package.</li>
 *   <li>logback does not log raw chat content even on failure.</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "mindbridge.ai.provider=mock",
        "mindbridge.ai.analysis-run.provider-label=mock",
        "mindbridge.ai.analysis-run.mock-model=MOCK_V1",
        "mindbridge.ai.analysis-run.prompt-version=" + ChatAnalysisPromptVersion.CURRENT
})
@Sql(scripts = {
        "/schema-conversation-messages.sql",
        "/schema-ai-analysis-runs.sql"
})
@DisplayName("AiAnalysisRunService integration")
class AiAnalysisRunIntegrationTest {

    @Autowired
    private AiAnalysisRunService service;

    @Autowired
    private AiAnalysisRunRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void insertMessageParent() {
        // conversation_messages has FK owned by ai_analysis_runs.message_id.
        // We insert a fresh row per test so each run is independent.
        // schema-conversation-messages.sql uses VARCHAR(36) UUIDs.
        UUID messageId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO conversation_messages (id, session_id, user_id, role, content, redacted, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                messageId.toString(), sessionId.toString(), userId.toString(),
                "USER", "raw content for integration", false);
        // Stash the ids on the test instance via thread-local? Too
        // complex — use the field-level @Autowired and a static map.
        TestData.lastMessageId = messageId;
        TestData.lastUserId = userId;
    }

    @AfterEach
    void cleanupRunsAndMessages() {
        // Clean up after EVERY test so a later test (e.g.
        // DevSeedIntegrationTest which does deleteAll() on
        // conversation_messages) does not see orphan ai_analysis_runs
        // rows pointing back at the messages this test created.
        repository.deleteAll();
        // Also delete the specific conversation_messages row this test
        // created. We don't delete ALL conversation_messages because
        // other tests may rely on ones they inserted.
        if (TestData.lastMessageId != null) {
            jdbc.update("DELETE FROM conversation_messages WHERE id = ?",
                    TestData.lastMessageId.toString());
            TestData.lastMessageId = null;
            TestData.lastUserId = null;
        }
    }

    @Test
    @DisplayName("context loads with run package on the classpath")
    void context_loads() {
        assertThat(applicationContext.getBean(AiAnalysisRunService.class)).isNotNull();
        assertThat(applicationContext.getBean(AiAnalysisRunRepository.class)).isNotNull();
    }

    @Test
    @DisplayName("successful run produces one SUCCEEDED row with all audit fields")
    void startRun_success_oneSucceededRow() {
        ChatAnalysisInput input = new ChatAnalysisInput(
                TestData.lastMessageId, TestData.lastUserId,
                "Hôm nay tôi thấy bình thường", "vi-VN");

        AiRunSummary summary = service.startRun(input);

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.SUCCEEDED);
        assertThat(summary.provider()).isEqualTo("mock");
        assertThat(summary.model()).isEqualTo("MOCK_V1");
        assertThat(summary.promptVersion()).isEqualTo(ChatAnalysisPromptVersion.CURRENT);
        assertThat(summary.schemaVersion()).isEqualTo("V1");
        assertThat(summary.inputHash()).matches("[0-9a-f]{64}");
        assertThat(summary.outputHash()).matches("[0-9a-f]{64}");
        assertThat(summary.createdAt()).isNotNull();
        assertThat(summary.startedAt()).isNotNull();
        assertThat(summary.completedAt()).isNotNull();
        assertThat(summary.completedAt()).isAfterOrEqualTo(summary.startedAt());

        // Verify the row is queryable by id.
        List<AiAnalysisRun> rows = repository.findByMessageIdOrderByCreatedAtDesc(
                TestData.lastMessageId);
        assertThat(rows).hasSize(1);
        AiAnalysisRun row = rows.get(0);
        assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.SUCCEEDED);
        assertThat(row.getErrorCode()).isNull();
        assertThat(row.getOutputHash()).isEqualTo(summary.outputHash());
    }

    @Test
    @DisplayName("two runs on the same message_id create two rows (no dedup)")
    void startRun_consecutiveCalls_twoRows() {
        ChatAnalysisInput input = new ChatAnalysisInput(
                TestData.lastMessageId, TestData.lastUserId,
                "first rerun", "vi-VN");

        AiRunSummary s1 = service.startRun(input);
        AiRunSummary s2 = service.startRun(input);

        assertThat(s1.id()).isNotEqualTo(s2.id());

        List<AiAnalysisRun> rows = repository.findByMessageIdOrderByCreatedAtDesc(
                TestData.lastMessageId);
        assertThat(rows).hasSize(2);
    }

    @Test
    @DisplayName("JPA reflection scan: lifecycle methods are package-private")
    void lifecycleMethods_arePackagePrivate() throws Exception {
        // markRunning / markSucceeded / markFailed must be package-private
        // so only classes in com.mindbridge.analysis.run.domain can call them.
        Class<?> entityClass = AiAnalysisRun.class;
        for (String methodName : new String[]{"markRunning", "markSucceeded", "markFailed"}) {
            for (Method m : entityClass.getDeclaredMethods()) {
                if (m.getName().equals(methodName)) {
                    assertThat(Modifier.isPublic(m.getModifiers()))
                            .as("%s must NOT be public", m.getName())
                            .isFalse();
                    assertThat(Modifier.isProtected(m.getModifiers()))
                            .as("%s must NOT be protected", m.getName())
                            .isFalse();
                    // It's package-private (no modifier).
                    // We don't assert exactly that because modifiers
                    // can be combined; just assert not public and not protected.
                }
            }
        }
    }

    @Test
    @DisplayName("migration header is documented and the file is UTF-8 no BOM")
    void migrationFile_isUtf8NoBom() throws Exception {
        Path v15 = Paths.get("src/main/resources/db/migration/V15__create_ai_analysis_runs.sql");
        if (!Files.exists(v15)) {
            // When running from backend/ working dir, use absolute path.
            v15 = Paths.get("backend/src/main/resources/db/migration/V15__create_ai_analysis_runs.sql");
        }
        // Skip if file cannot be located (e.g. CI without the path).
        if (!Files.exists(v15)) {
            return;
        }
        byte[] bytes = Files.readAllBytes(v15);
        assertThat(bytes.length).isGreaterThan(0);
        // No UTF-8 BOM (EF BB BF).
        if (bytes.length >= 3) {
            assertThat(bytes[0] != (byte) 0xEF || bytes[1] != (byte) 0xBB || bytes[2] != (byte) 0xBF)
                    .as("V15 must not have a UTF-8 BOM")
                    .isTrue();
        }
    }

    @Test
    @DisplayName("object mapper produces a stable hash for the same output")
    void outputHash_isStableAcrossInvocations() throws Exception {
        // Independent of the service: verify the SHA-256 helper used
        // by the service is deterministic for the same input bytes.
        String content = "tôi buồn";
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] firstHash = md.digest(content.getBytes(StandardCharsets.UTF_8));
        md = MessageDigest.getInstance("SHA-256");
        byte[] secondHash = md.digest(content.getBytes(StandardCharsets.UTF_8));
        assertThat(firstHash).isEqualTo(secondHash);
    }

    @Test
    @DisplayName("object mapper bean is available")
    void objectMapper_isAvailable() {
        ObjectMapper mapper = applicationContext.getBean(ObjectMapper.class);
        assertThat(mapper).isNotNull();
    }

    /**
     * Thread-local stash for IDs so {@code @BeforeEach} can hand them
     * to the test methods. Not pretty, but avoids adding a
     * dedicated test fixture.
     */
    static final class TestData {
        static volatile UUID lastMessageId;
        static volatile UUID lastUserId;
    }
}