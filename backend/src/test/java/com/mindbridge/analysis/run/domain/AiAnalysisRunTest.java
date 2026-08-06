package com.mindbridge.analysis.run.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AiAnalysisRun} entity factory and lifecycle
 * transition methods. Pure JUnit 5 + AssertJ Ã¢â‚¬â€ no Spring, no DB.
 *
 * <p>Tests are organised by entity responsibility:
 * <ul>
 *   <li>{@code createPending_*} Ã¢â‚¬â€ factory invariants.</li>
 *   <li>{@code markRunning_*} Ã¢â‚¬â€ PENDING Ã¢â€ â€™ RUNNING transition.</li>
 *   <li>{@code markSucceeded_*} Ã¢â‚¬â€ RUNNING Ã¢â€ â€™ SUCCEEDED transition.</li>
 *   <li>{@code markFailed_*} Ã¢â‚¬â€ PENDING/RUNNING Ã¢â€ â€™ FAILED transition.</li>
 *   <li>{@code schemaVersion_*} Ã¢â‚¬â€ the schema version constant.</li>
 * </ul>
 */
@DisplayName("AiAnalysisRun entity")
class AiAnalysisRunTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID MESSAGE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PROVIDER = "mock";
    private static final String MODEL = "MOCK_V1";
    private static final String PROMPT_VERSION = "v1:5363675e22fe";
    private static final String INPUT_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";
    private static final OffsetDateTime CREATED =
            OffsetDateTime.of(2026, 8, 2, 1, 0, 0, 0, ZoneOffset.UTC);

    private static AiAnalysisRun newPending() {
        return AiAnalysisRun.createPending(
                ID, MESSAGE_ID, USER_ID, PROVIDER, MODEL, PROMPT_VERSION,
                INPUT_HASH, CREATED);
    }

    // --- createPending invariants ---

    @Nested
    @DisplayName("createPending")
    class CreatePending {

        @Test
        @DisplayName("assigns all immutable fields and sets status=PENDING")
        void createPending_assignsAllFieldsSetsPendingStatus() {
            AiAnalysisRun row = newPending();

            assertThat(row.getId()).isEqualTo(ID);
            assertThat(row.getMessageId()).isEqualTo(MESSAGE_ID);
            assertThat(row.getUserId()).isEqualTo(USER_ID);
            assertThat(row.getProvider()).isEqualTo(PROVIDER);
            assertThat(row.getModel()).isEqualTo(MODEL);
            assertThat(row.getPromptVersion()).isEqualTo(PROMPT_VERSION);
            assertThat(row.getInputHash()).isEqualTo(INPUT_HASH);
            assertThat(row.getCreatedAt()).isEqualTo(CREATED);
            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.PENDING);
            assertThat(row.getSchemaVersion()).isEqualTo(
                    AiAnalysisRun.CURRENT_SCHEMA_VERSION);
        }

        @Test
        @DisplayName("leaves lifecycle fields null on PENDING")
        void createPending_lifecycleFieldsAreNull() {
            AiAnalysisRun row = newPending();

            assertThat(row.getOutputHash()).isNull();
            assertThat(row.getErrorCode()).isNull();
            assertThat(row.getErrorSummary()).isNull();
            assertThat(row.getLatencyMs()).isZero();
            assertThat(row.getStartedAt()).isNull();
            assertThat(row.getCompletedAt()).isNull();
            assertThat(row.getInputTokens()).isNull();
            assertThat(row.getOutputTokens()).isNull();
            assertThat(row.getModelRiskLevel()).isNull();
            assertThat(row.getConfidence()).isNull();
        }

        @Test
        @DisplayName("rejects null id")
        void createPending_nullId_throws() {
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    null, MESSAGE_ID, USER_ID, PROVIDER, MODEL, PROMPT_VERSION,
                    INPUT_HASH, CREATED))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null input hash")
        void createPending_nullInputHash_throws() {
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    ID, MESSAGE_ID, USER_ID, PROVIDER, MODEL, PROMPT_VERSION,
                    null, CREATED))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects input hash not 64 chars")
        void createPending_shortInputHash_throws() {
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    ID, MESSAGE_ID, USER_ID, PROVIDER, MODEL, PROMPT_VERSION,
                    "tooshort", CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inputHash");
        }

        @Test
        @DisplayName("rejects input hash with non-hex characters")
        void createPending_nonHexInputHash_throws() {
            String badHash = "g".repeat(64);
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    ID, MESSAGE_ID, USER_ID, PROVIDER, MODEL, PROMPT_VERSION,
                    badHash, CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inputHash");
        }

        @Test
        @DisplayName("rejects blank provider")
        void createPending_blankProvider_throws() {
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    ID, MESSAGE_ID, USER_ID, "   ", MODEL, PROMPT_VERSION,
                    INPUT_HASH, CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("provider");
        }

        @Test
        @DisplayName("rejects provider string longer than 50 chars")
        void createPending_tooLongProvider_throws() {
            assertThatThrownBy(() -> AiAnalysisRun.createPending(
                    ID, MESSAGE_ID, USER_ID, "x".repeat(51), MODEL, PROMPT_VERSION,
                    INPUT_HASH, CREATED))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("provider");
        }
    }

    // --- markRunning transitions ---

    @Nested
    @DisplayName("markRunning")
    class MarkRunning {

        @Test
        @DisplayName("transitions PENDING -> RUNNING and sets startedAt")
        void markRunning_transitionsAndSetsStartedAt() {
            AiAnalysisRun row = newPending();
            OffsetDateTime started = CREATED.plusSeconds(1);

            row.markRunning(started);

            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.RUNNING);
            assertThat(row.getStartedAt()).isEqualTo(started);
        }

        @Test
        @DisplayName("is idempotent on already-RUNNING row")
        void markRunning_idempotent() {
            AiAnalysisRun row = newPending();
            OffsetDateTime started = CREATED.plusSeconds(1);

            row.markRunning(started);
            row.markRunning(started.plusSeconds(1));

            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.RUNNING);
            assertThat(row.getStartedAt()).isEqualTo(started);
        }

        @Test
        @DisplayName("rejects startedAt < createdAt")
        void markRunning_rejectsStartedAtBeforeCreatedAt() {
            AiAnalysisRun row = newPending();
            OffsetDateTime started = CREATED.minusSeconds(1);

            assertThatThrownBy(() -> row.markRunning(started))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("startedAt");
        }
    }

    // --- markSucceeded transitions ---

    @Nested
    @DisplayName("markSucceeded")
    class MarkSucceeded {

        private static final String OUTPUT_HASH =
                "1111111111111111111111111111111111111111111111111111111111111111";

        @Test
        @DisplayName("transitions RUNNING -> SUCCEEDED and sets all fields")
        void markSucceeded_transitionsAndSetsAllFields() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            OffsetDateTime completed = CREATED.plusSeconds(2);
            row.markSucceeded(OUTPUT_HASH, 1500, 100L, 80L,
                    (short) 3, new BigDecimal("0.85"), completed);

            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.SUCCEEDED);
            assertThat(row.getOutputHash()).isEqualTo(OUTPUT_HASH);
            assertThat(row.getLatencyMs()).isEqualTo(1500);
            assertThat(row.getInputTokens()).isEqualTo(100L);
            assertThat(row.getOutputTokens()).isEqualTo(80L);
            assertThat(row.getModelRiskLevel()).isEqualTo((short) 3);
            assertThat(row.getConfidence()).isEqualByComparingTo("0.85");
            assertThat(row.getCompletedAt()).isEqualTo(completed);
            assertThat(row.getErrorCode()).isNull();
            assertThat(row.getErrorSummary()).isNull();
        }

        @Test
        @DisplayName("rejects output hash not 64 chars")
        void markSucceeded_badOutputHash_throws() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            assertThatThrownBy(() -> row.markSucceeded(
                    "tooshort", 100, null, null, null, null,
                    CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("outputHash");
        }

        @Test
        @DisplayName("rejects modelRiskLevel out of range")
        void markSucceeded_badRiskLevel_throws() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            assertThatThrownBy(() -> row.markSucceeded(
                    OUTPUT_HASH, 100, null, null, (short) 5, null,
                    CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("modelRiskLevel");
        }

        @Test
        @DisplayName("rejects confidence out of range")
        void markSucceeded_badConfidence_throws() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            assertThatThrownBy(() -> row.markSucceeded(
                    OUTPUT_HASH, 100, null, null, null,
                    new BigDecimal("1.5"), CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("confidence");
        }

        @Test
        @DisplayName("rejects negative latency")
        void markSucceeded_negativeLatency_throws() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            assertThatThrownBy(() -> row.markSucceeded(
                    OUTPUT_HASH, -1, null, null, null, null,
                    CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("latencyMs");
        }

        @Test
        @DisplayName("rejects completedAt < startedAt")
        void markSucceeded_completedBeforeStarted_throws() {
            AiAnalysisRun row = newPending();
            OffsetDateTime started = CREATED.plusSeconds(30);
            row.markRunning(started);

            assertThatThrownBy(() -> row.markSucceeded(
                    OUTPUT_HASH, 100, null, null, null, null,
                    CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("completedAt");
        }
    }

    // --- markFailed transitions ---

    @Nested
    @DisplayName("markFailed")
    class MarkFailed {

        @Test
        @DisplayName("transitions PENDING -> FAILED with error code and summary")
        void markFailed_transitionsPendingToFailed() {
            AiAnalysisRun row = newPending();

            OffsetDateTime failedAt = CREATED.plusSeconds(1);
            row.markFailed("AI_PROVIDER_TIMEOUT", "TIMEOUT", 1000, failedAt);

            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.FAILED);
            assertThat(row.getErrorCode()).isEqualTo("AI_PROVIDER_TIMEOUT");
            assertThat(row.getErrorSummary()).isEqualTo("TIMEOUT");
            assertThat(row.getLatencyMs()).isEqualTo(1000L);
            assertThat(row.getCompletedAt()).isEqualTo(failedAt);
            assertThat(row.getOutputHash()).isNull();
        }

        @Test
        @DisplayName("transitions RUNNING -> FAILED with error code and summary")
        void markFailed_transitionsRunningToFailed() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));

            OffsetDateTime failedAt = CREATED.plusSeconds(2);
            row.markFailed("AI_ANALYSIS_OUTPUT_INVALID", "INVALID", 500, failedAt);

            assertThat(row.getStatus()).isEqualTo(AiAnalysisRunStatus.FAILED);
            assertThat(row.getErrorCode()).isEqualTo("AI_ANALYSIS_OUTPUT_INVALID");
        }

        @Test
        @DisplayName("rejects unknown error code")
        void markFailed_badErrorCode_throws() {
            AiAnalysisRun row = newPending();

            assertThatThrownBy(() -> row.markFailed(
                    "BAD_CODE", "msg", 0, CREATED.plusSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorCode");
        }

        @Test
        @DisplayName("rejects error summary longer than 200 chars")
        void markFailed_tooLongErrorSummary_throws() {
            AiAnalysisRun row = newPending();

            String tooLong = "x".repeat(201);
            assertThatThrownBy(() -> row.markFailed(
                    "AI_PROVIDER_TIMEOUT", tooLong, 0, CREATED.plusSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("errorSummary");
        }

        @Test
        @DisplayName("rejects transition from terminal FAILED")
        void markFailed_cannotTransitionFromFailed() {
            AiAnalysisRun row = newPending();
            row.markFailed("AI_PROVIDER_TIMEOUT", "msg", 100, CREATED.plusSeconds(1));

            assertThatThrownBy(() -> row.markFailed(
                    "AI_PROVIDER_UNAVAILABLE", "msg2", 200, CREATED.plusSeconds(2)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal");
        }

        @Test
        @DisplayName("rejects transition from terminal SUCCEEDED")
        void markFailed_cannotTransitionFromSucceeded() {
            AiAnalysisRun row = newPending();
            row.markRunning(CREATED.plusSeconds(1));
            row.markSucceeded(
                    "1111111111111111111111111111111111111111111111111111111111111111",
                    100, null, null, null, null, CREATED.plusSeconds(2));

            assertThatThrownBy(() -> row.markFailed(
                    "AI_PROVIDER_TIMEOUT", "msg", 200, CREATED.plusSeconds(3)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("terminal");
        }
    }

    // --- status enum helpers ---

    @Nested
    @DisplayName("AiAnalysisRunStatus.isTerminal")
    class StatusTerminal {

        @Test
        @DisplayName("PENDING is not terminal")
        void pending_isNotTerminal() {
            assertThat(AiAnalysisRunStatus.PENDING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("RUNNING is not terminal")
        void running_isNotTerminal() {
            assertThat(AiAnalysisRunStatus.RUNNING.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("SUCCEEDED is terminal")
        void succeeded_isTerminal() {
            assertThat(AiAnalysisRunStatus.SUCCEEDED.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED is terminal")
        void failed_isTerminal() {
            assertThat(AiAnalysisRunStatus.FAILED.isTerminal()).isTrue();
        }
    }

    // --- schema version constant ---

    @Test
    @DisplayName("CURRENT_SCHEMA_VERSION is V1")
    void currentSchemaVersion_isV1() {
        assertThat(AiAnalysisRun.CURRENT_SCHEMA_VERSION).isEqualTo("V1");
    }
}