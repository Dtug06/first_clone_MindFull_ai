package com.mindbridge.analysis.run.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.MockScenario;
import com.mindbridge.analysis.provider.RealLlmResponseContext;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisPromptVersion;
import com.mindbridge.analysis.run.dto.AiRunSummary;
import com.mindbridge.analysis.run.repository.AiAnalysisRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unit tests for {@link AiAnalysisRunService} with mocked
 * {@link ChatAnalysisProvider} and {@link AiAnalysisRunRepository}.
 *
 * <p>These tests verify the lifecycle orchestration without booting
 * Spring. The {@code @Transactional} propagation on the service's
 * helper methods is effectively a no-op here (no Spring proxy when
 * called from the same class), and the repository is a mock so the
 * DB is never touched.
 *
 * <p>Covers DoD §4.1 (one row per call), §4.2 (no raw chat on
 * failure), §4.3 (provider/model/prompt/schema recorded).
 */
@DisplayName("AiAnalysisRunService")
class AiAnalysisRunServiceTest {

    private ChatAnalysisProvider provider;
    private AiAnalysisRunRepository repository;
    private AiAnalysisRunService service;
    private ObjectMapper objectMapper;
    private Clock fixedClock;

    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String CONTENT = "Hôm nay tôi thấy bình thường";
    private static final String PROVIDER_LABEL = "mock";
    private static final String MODEL_LABEL = "MOCK_V1";

    @BeforeEach
    void setUp() {
        provider = org.mockito.Mockito.mock(ChatAnalysisProvider.class);
        repository = org.mockito.Mockito.mock(AiAnalysisRunRepository.class);
        objectMapper = new ObjectMapper();
        fixedClock = Clock.fixed(
                Instant.parse("2026-08-02T01:00:00Z"), ZoneOffset.UTC);
        // Simulate a real repo: track persisted entities by id so
        // findById returns the SAME instance we saved. Expose the
        // store via a static so tests can capture state at each save.
        store.clear();
        org.mockito.stubbing.Answer<AiAnalysisRun> saveAnswer = inv -> {
            AiAnalysisRun row = inv.getArgument(0);
            store.put(row.getId(), row);
            return row;
        };
        when(repository.save(any(AiAnalysisRun.class))).thenAnswer(saveAnswer);
        when(repository.findById(any(UUID.class)))
                .thenAnswer(inv -> {
                    UUID id = inv.getArgument(0);
                    return Optional.ofNullable(store.get(id));
                });
        service = new AiAnalysisRunService(
                provider, repository, objectMapper, fixedClock,
                PROVIDER_LABEL, MODEL_LABEL, ChatAnalysisPromptVersion.CURRENT);
    }

    /** Shared in-memory store across all test methods. */
    private static final java.util.Map<UUID, AiAnalysisRun> store = new java.util.HashMap<>();

    private ChatAnalysisInput input() {
        return new ChatAnalysisInput(MESSAGE_ID, USER_ID, CONTENT, "vi-VN");
    }

    private ChatAnalysisOutput okOutput() {
        return new ChatAnalysisOutput(
                Topic.WORK_STRESS, Emotion.NEUTRAL, Intent.VENT,
                List.of(),
                1, 0.72,
                List.of(),
                15L,
                null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
    }

    // --- DoD §4.1: one row per call ---

    @Test
    @DisplayName("succeed: 3 save() calls (PENDING, RUNNING, SUCCEEDED) with correct fields")
    void startRun_success_createsRowWithStatusPendingAndSucceeds() {
        when(provider.analyze(any())).thenReturn(okOutput());

        AiRunSummary summary = service.startRun(input());

        // The mock recorded 3 save() calls. Each save updates the
        // in-memory store with the same instance, so the final state
        // is SUCCEEDED. However, the SETUP wrote to the store at each
        // save, so calling entity mutations then save again worked.
        // Verification via the returned summary is the cleanest way.
        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.SUCCEEDED);
        assertThat(summary.messageId()).isEqualTo(MESSAGE_ID);
        assertThat(summary.userId()).isEqualTo(USER_ID);
        assertThat(summary.provider()).isEqualTo(PROVIDER_LABEL);
        assertThat(summary.model()).isEqualTo(MODEL_LABEL);
        assertThat(summary.promptVersion()).isEqualTo(ChatAnalysisPromptVersion.CURRENT);
        assertThat(summary.inputHash()).hasSize(64);
        assertThat(summary.inputHash()).matches("[0-9a-f]{64}");
        assertThat(summary.schemaVersion()).isEqualTo("V1");
        assertThat(summary.outputHash()).hasSize(64);
        assertThat(summary.outputHash()).matches("[0-9a-f]{64}");
        assertThat(summary.modelRiskLevel()).isEqualTo((short) 1);
        assertThat(summary.confidence()).isEqualByComparingTo("0.72");
        assertThat(summary.errorCode()).isNull();
        assertThat(summary.errorSummary()).isNull();
        assertThat(summary.completedAt()).isNotNull();

        // 3 save calls total for one successful run.
        verify(repository, times(3)).save(any(AiAnalysisRun.class));
    }

    @Test
    @DisplayName("DoD §4.1: every startRun call creates exactly one row")
    void startRun_calledTwice_createsTwoRows() {
        when(provider.analyze(any())).thenReturn(okOutput());

        AiRunSummary s1 = service.startRun(input());
        AiRunSummary s2 = service.startRun(input());

        assertThat(s1.id()).isNotEqualTo(s2.id());
        verify(repository, times(6)).save(any(AiAnalysisRun.class));
    }

    // --- DoD §4.2: failure path stores error code, no raw chat ---

    @Test
    @DisplayName("timeout: marks FAILED with AI_PROVIDER_TIMEOUT, no raw chat in summary")
    void startRun_providerTimeout_marksFailedWithTimeoutCode() {
        when(provider.analyze(any())).thenThrow(new ProviderTimeoutException());

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.FAILED);
        assertThat(summary.errorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
        assertThat(summary.errorSummary()).isNotNull();
        // DoD §4.2: no raw chat content in the summary.
        assertThat(summary.errorSummary()).doesNotContain(CONTENT);
        assertThat(summary.errorSummary()).doesNotContain("Hôm nay");
        assertThat(summary.completedAt()).isNotNull();
        assertThat(summary.outputHash()).isNull();
    }

    @Test
    @DisplayName("unavailable: marks FAILED with AI_PROVIDER_UNAVAILABLE")
    void startRun_providerUnavailable_marksFailedWithUnavailableCode() {
        when(provider.analyze(any())).thenThrow(new ProviderUnavailableException());

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.FAILED);
        assertThat(summary.errorCode()).isEqualTo("AI_PROVIDER_UNAVAILABLE");
        assertThat(summary.errorSummary()).doesNotContain(CONTENT);
    }

    @Test
    @DisplayName("invalid output: marks FAILED with AI_ANALYSIS_OUTPUT_INVALID")
    void startRun_invalidOutput_marksFailedWithInvalidCode() {
        when(provider.analyze(any())).thenThrow(new InvalidAnalysisOutputException());

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.FAILED);
        assertThat(summary.errorCode()).isEqualTo("AI_ANALYSIS_OUTPUT_INVALID");
        assertThat(summary.errorSummary()).doesNotContain(CONTENT);
    }

    @Test
    @DisplayName("unexpected RuntimeException: marks FAILED with AI_ANALYSIS_OUTPUT_INVALID")
    void startRun_unexpectedException_marksFailedAsInvalidOutput() {
        when(provider.analyze(any())).thenThrow(new RuntimeException("boom"));

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.status()).isEqualTo(AiAnalysisRunStatus.FAILED);
        assertThat(summary.errorCode()).isEqualTo("AI_ANALYSIS_OUTPUT_INVALID");
        // DoD §4.2: no raw chat content in the summary.
        assertThat(summary.errorSummary()).doesNotContain(CONTENT);
        // Note: the exception message ("boom") is ASCII and is part
        // of the diagnostic info we DO want to surface. The redactor
        // strips non-ASCII (Vietnamese) so raw chat content cannot
        // leak. The strong guarantee is "no raw chat", not "no
        // exception message".
        assertThat(summary.errorSummary()).doesNotContain("Hôm nay");
    }

    // --- DoD §4.3: provider/model/prompt/schema are recorded ---

    @Test
    @DisplayName("DoD §4.3: every persisted row carries provider/model/prompt/schema")
    void startRun_recordsAllMetadataFields() {
        when(provider.analyze(any())).thenReturn(okOutput());

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.provider()).isEqualTo("mock");
        assertThat(summary.model()).isEqualTo("MOCK_V1");
        assertThat(summary.promptVersion()).isEqualTo("v1:5363675e22fe");
        assertThat(summary.schemaVersion()).isEqualTo("V1");
        assertThat(summary.inputHash()).matches("[0-9a-f]{64}");
        assertThat(summary.outputHash()).matches("[0-9a-f]{64}");
    }

    // --- Input validation ---

    @Test
    @DisplayName("null input throws IllegalArgumentException")
    void startRun_nullInput_throws() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.startRun(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Determinism constraint: same input → same output hash ---

    @Test
    @DisplayName("deterministic: same input produces same output hash")
    void startRun_sameInput_sameOutputHash() {
        when(provider.analyze(any())).thenReturn(okOutput());

        AiRunSummary s1 = service.startRun(input());
        AiRunSummary s2 = service.startRun(input());

        // Different run IDs, but same provider/model/prompt/schema/input/output hashes.
        assertThat(s1.outputHash()).isEqualTo(s2.outputHash());
        assertThat(s1.inputHash()).isEqualTo(s2.inputHash());
    }

    // --- G3-T06: Real provider path ---

    @Test
    @DisplayName("G3-T06: real provider sets snapshot → row's provider/model updated to response values")
    void startRun_realProvider_overridesProviderAndModelFromSnapshot() {
        // The mock provider path is replaced by a fake that simulates a
        // real provider: returns a valid output AND populates the
        // response context with the response-time labels.
        com.mindbridge.analysis.provider.ChatAnalysisProvider realFake =
                new com.mindbridge.analysis.provider.ChatAnalysisProvider() {
                    @Override
                    public com.mindbridge.analysis.provider.ChatAnalysisOutput analyze(
                            com.mindbridge.analysis.provider.ChatAnalysisInput input) {
                        RealLlmResponseContext.set(
                                new RealLlmResponseContext.Snapshot(
                                        "openai",
                                        "gpt-4o-mini-2024-07-18"));
                        return okOutput();
                    }
                };
        AiAnalysisRunService realService = new AiAnalysisRunService(
                realFake, repository, objectMapper, fixedClock,
                "real-fallback", "REAL_PLACEHOLDER", ChatAnalysisPromptVersion.CURRENT);

        AiRunSummary summary = realService.startRun(input());

        assertThat(summary.provider()).isEqualTo("openai");
        assertThat(summary.model()).isEqualTo("gpt-4o-mini-2024-07-18");
        // Verify the context was cleared (no leak across calls).
        assertThat(RealLlmResponseContext.current()).isNull();
    }

    @Test
    @DisplayName("G3-T06: mock provider sets no snapshot → provider/model stay as constructor labels")
    void startRun_mockProvider_keepsConstructorLabels() {
        when(provider.analyze(any())).thenReturn(okOutput());

        AiRunSummary summary = service.startRun(input());

        assertThat(summary.provider()).isEqualTo("mock");
        assertThat(summary.model()).isEqualTo("MOCK_V1");
        assertThat(RealLlmResponseContext.current()).isNull();
    }
}