package com.mindbridge.analysis.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.result.service.ChatAnalysisResultService;
import com.mindbridge.analysis.run.domain.AiAnalysisRunService;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;
import com.mindbridge.analysis.run.dto.AiAnalysisExecutionResult;
import com.mindbridge.analysis.run.dto.AiRunSummary;
import com.mindbridge.chat.dto.ChatMessageResponse.AnalysisStatus;
import com.mindbridge.consent.service.ConsentGuard;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatAnalysisPipelineServiceTest {

    @Mock ConsentGuard consentGuard;
    @Mock AiAnalysisRunService runService;
    @Mock ChatAnalysisResultService resultService;

    private ChatAnalysisPipelineService service;

    @BeforeEach
    void setUp() {
        service = new ChatAnalysisPipelineService(consentGuard, runService, resultService);
    }

    @Test
    void successfulRunPersistsAuthoritativeResult() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID runId = UUID.randomUUID();
        ChatAnalysisOutput output = mock(ChatAnalysisOutput.class);
        AiRunSummary run = summary(runId, messageId, userId, AiAnalysisRunStatus.SUCCEEDED);
        when(runService.execute(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiAnalysisExecutionResult(run, output));

        AnalysisStatus status = service.analyze(messageId, userId, "processed");

        assertThat(status).isEqualTo(AnalysisStatus.SUCCEEDED);
        verify(consentGuard).requireChatAnalysisConsent(userId);
        verify(resultService).recordResult(runId, output);
    }

    @Test
    void failedRunDoesNotCreateResult() {
        UUID messageId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        AiRunSummary run = summary(UUID.randomUUID(), messageId, userId, AiAnalysisRunStatus.FAILED);
        when(runService.execute(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AiAnalysisExecutionResult(run, null));

        assertThat(service.analyze(messageId, userId, "processed"))
                .isEqualTo(AnalysisStatus.FAILED);
        verifyNoInteractions(resultService);
    }

    private AiRunSummary summary(
            UUID runId, UUID messageId, UUID userId, AiAnalysisRunStatus status) {
        return new AiRunSummary(
                runId, messageId, userId, "mock", "MOCK_V1", "v1", "v1",
                status, "input", null, null, null, 0,
                null, null, null, null, null, null, null);
    }
}
