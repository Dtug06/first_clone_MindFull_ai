package com.mindbridge.analysis.run.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.provider.AnalysisSchemaVersion;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.Emotion;
import com.mindbridge.analysis.provider.Intent;
import com.mindbridge.analysis.provider.Signal;
import com.mindbridge.analysis.provider.Topic;
import com.mindbridge.analysis.result.domain.ChatAnalysisResult;
import com.mindbridge.analysis.result.domain.ResultAnalysisStatus;
import com.mindbridge.analysis.result.dto.ChatAnalysisResultSummary;
import com.mindbridge.analysis.result.exception.ChatAnalysisResultStateException;
import com.mindbridge.analysis.result.repository.ChatAnalysisResultRepository;
import com.mindbridge.analysis.result.service.ChatAnalysisResultService;
import com.mindbridge.analysis.run.repository.AiAnalysisRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ChatAnalysisResultService")
class ChatAnalysisResultServiceTest {

    private ChatAnalysisResultRepository resultRepository;
    private AiAnalysisRunRepository runRepository;
    private ChatAnalysisResultService service;
    private Clock fixedClock;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String INPUT_HASH = "a".repeat(64);
    private static final String OUTPUT_HASH = "b".repeat(64);
    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.of(2026, 8, 2, 12, 0, 0, 0, ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        resultRepository = org.mockito.Mockito.mock(ChatAnalysisResultRepository.class);
        runRepository = org.mockito.Mockito.mock(AiAnalysisRunRepository.class);
        fixedClock = Clock.fixed(Instant.parse("2026-08-02T12:00:00Z"), ZoneOffset.UTC);
        service = new ChatAnalysisResultService(resultRepository, runRepository, fixedClock);

        store.clear();
        doAnswer(inv -> {
            ChatAnalysisResult row = inv.getArgument(0);
            store.put(row.getId(), row);
            return row;
        }).when(resultRepository).save(any(ChatAnalysisResult.class));

        when(resultRepository.findById(any(UUID.class)))
                .thenAnswer(inv -> Optional.ofNullable(store.get(inv.getArgument(0))));
    }

    private static final Map<UUID, ChatAnalysisResult> store = new HashMap<>();

    @Nested
    class RecordResult {

        @Test
        @DisplayName("SUCCEEDED run: creates one new ACTIVE row")
        void recordResult_succeeded_createsOneActiveRow() {
            UUID runId = UUID.randomUUID();
            when(runRepository.findById(runId))
                    .thenReturn(Optional.of(succeededRun(runId, UUID.randomUUID(), USER_ID)));

            ChatAnalysisResultSummary summary = service.recordResult(runId, okOutput());

            org.mockito.Mockito.verify(resultRepository, times(1)).save(any(ChatAnalysisResult.class));
            assertThat(summary.analysisStatus()).isEqualTo(ResultAnalysisStatus.ACTIVE);
            assertThat(summary.modelRiskLevel()).isEqualTo((short) 1);
            assertThat(summary.supersedesId()).isNull();
        }

        @Test
        @DisplayName("DoD 4.1: first record - no existing ACTIVE, supersedesId = null")
        void recordResult_firstRun_noSupersedesChain() {
            UUID messageId = UUID.randomUUID();
            UUID runId = UUID.randomUUID();
            when(runRepository.findById(runId))
                    .thenReturn(Optional.of(succeededRun(runId, messageId, USER_ID)));
            when(resultRepository.findEffectiveByConversationMessageId(messageId))
                    .thenReturn(Optional.empty());

            ChatAnalysisResultSummary summary = service.recordResult(runId, okOutput());

            assertThat(summary.supersedesId()).isNull();
            assertThat(summary.analysisStatus()).isEqualTo(ResultAnalysisStatus.ACTIVE);
        }

        @Test
        @DisplayName("non-SUCCEEDED run throws IllegalStateException")
        void recordResult_runNotSucceeded_throws() {
            UUID runId = UUID.randomUUID();
            AiAnalysisRun pendingRun = AiAnalysisRun.createPending(
                    runId, UUID.randomUUID(), USER_ID, "mock", "MOCK", "v1",
                    INPUT_HASH, FIXED_TIME);
            when(runRepository.findById(runId)).thenReturn(Optional.of(pendingRun));

            assertThatThrownBy(() -> service.recordResult(runId, okOutput()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("non-SUCCEEDED");
        }

        @Test
        @DisplayName("null runId throws IllegalArgumentException")
        void recordResult_nullRunId_throws() {
            assertThatThrownBy(() -> service.recordResult(null, okOutput()))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null output throws IllegalArgumentException")
        void recordResult_nullOutput_throws() {
            assertThatThrownBy(() -> service.recordResult(UUID.randomUUID(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("run not found throws IllegalStateException")
        void recordResult_runNotFound_throws() {
            when(runRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.recordResult(UUID.randomUUID(), okOutput()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Run not found");
        }

        @Test
        @DisplayName("stores modelRiskLevel and confidence correctly")
        void recordResult_storesRiskAndConfidence() {
            UUID messageId = UUID.randomUUID();
            UUID runId = UUID.randomUUID();
            when(runRepository.findById(runId))
                    .thenReturn(Optional.of(succeededRun(runId, messageId, USER_ID)));
            when(resultRepository.findEffectiveByConversationMessageId(messageId))
                    .thenReturn(Optional.empty());

            ChatAnalysisOutput output = new ChatAnalysisOutput(
                    Topic.HEALTH, Emotion.DISTRESS, Intent.SUPPORT,
                    List.of(Signal.SELF_HARM_RISK, Signal.HOPELESSNESS),
                    4, 0.95,
                    List.of(), 30L, null,
                    AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

            ChatAnalysisResultSummary summary = service.recordResult(runId, output);

            assertThat(summary.modelRiskLevel()).isEqualTo((short) 4);
            assertThat(summary.confidence()).isEqualByComparingTo("0.95");
            assertThat(summary.signals()).containsExactly("SELF_HARM_RISK", "HOPELESSNESS");
        }
    }

    @Nested
    class GetEffectiveResult {

        @Test
        @DisplayName("returns empty Optional when no ACTIVE row")
        void getEffectiveResult_noActive_returnsEmpty() {
            UUID messageId = UUID.randomUUID();
            when(resultRepository.findEffectiveByConversationMessageId(messageId))
                    .thenReturn(Optional.empty());

            Optional<ChatAnalysisResultSummary> result = service.getEffectiveResult(messageId);
            assertThat(result).isEmpty();
        }
    }

    @Nested
    class InvalidateResult {

        @Test
        @DisplayName("null resultId throws IllegalArgumentException")
        void invalidateResult_nullId_throws() {
            assertThatThrownBy(() -> service.invalidateResult(null, "test"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static ChatAnalysisOutput okOutput() {
        return new ChatAnalysisOutput(
                Topic.WORK_STRESS, Emotion.NEUTRAL, Intent.VENT,
                List.of(), 1, 0.72,
                List.of(), 15L, null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);
    }

    private AiAnalysisRun succeededRun(UUID runId, UUID messageId, UUID userId) {
        return AiAnalysisRunTestHelper.succeededRun(
                runId, messageId, userId,
                "mock", "MOCK", "v1",
                INPUT_HASH, OUTPUT_HASH, FIXED_TIME);
    }
}