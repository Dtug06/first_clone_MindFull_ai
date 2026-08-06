package com.mindbridge.analysis.pipeline;

import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.result.service.ChatAnalysisResultService;
import com.mindbridge.analysis.run.domain.AiAnalysisRunService;
import com.mindbridge.analysis.run.dto.AiAnalysisExecutionResult;
import com.mindbridge.chat.dto.ChatMessageResponse.AnalysisStatus;
import com.mindbridge.consent.service.ConsentGuard;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Connects a consented, preprocessed chat message to the G3 structured
 * analysis lifecycle and its authoritative result row.
 */
@Service
public class ChatAnalysisPipelineService {

    private static final Logger log = LoggerFactory.getLogger(ChatAnalysisPipelineService.class);

    private final ConsentGuard consentGuard;
    private final AiAnalysisRunService runService;
    private final ChatAnalysisResultService resultService;

    public ChatAnalysisPipelineService(
            ConsentGuard consentGuard,
            AiAnalysisRunService runService,
            ChatAnalysisResultService resultService) {
        this.consentGuard = consentGuard;
        this.runService = runService;
        this.resultService = resultService;
    }

    public AnalysisStatus analyze(UUID messageId, UUID userId, String processedContent) {
        consentGuard.requireChatAnalysisConsent(userId);
        AiAnalysisExecutionResult execution = runService.execute(new ChatAnalysisInput(
                messageId, userId, processedContent, ChatAnalysisInput.DEFAULT_LOCALE));
        if (!execution.succeeded()) {
            return AnalysisStatus.FAILED;
        }
        try {
            resultService.recordResult(execution.run().id(), execution.output());
            return AnalysisStatus.SUCCEEDED;
        } catch (RuntimeException ex) {
            log.warn("Unable to persist structured chat analysis for messageId={} runId={} causeClass={}",
                    messageId, execution.run().id(), ex.getClass().getSimpleName());
            return AnalysisStatus.FAILED;
        }
    }
}
