package com.mindbridge.chat.service;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.exception.ProviderUnavailableException;
import com.mindbridge.analysis.pipeline.ChatAnalysisPipelineService;
import com.mindbridge.chat.ai.ConversationResponseInput;
import com.mindbridge.chat.ai.ConversationResponseProvider;
import com.mindbridge.chat.dto.ChatMessageResponse;
import com.mindbridge.chat.dto.ChatTurnResponse;
import com.mindbridge.chat.dto.ChatTurnResponse.ReplyStatus;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.response.executor.SafetyResponseTemplateExecutor;
import com.mindbridge.safety.response.executor.SafetyResponseTemplateExecutor.ResolvedResponse;
import com.mindbridge.safety.event.service.SafetyEventService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Orchestrates one user message and one optional assistant response.
 *
 * <p>The class is deliberately not transactional: user persistence and Safety
 * evaluation complete before a potentially slow external response-provider
 * call starts. Assistant persistence is a separate short transaction.
 */
@Service
public class ConversationTurnService {

    private static final Logger log = LoggerFactory.getLogger(ConversationTurnService.class);

    private final ConversationMessageService messageService;
    private final ConversationResponseProvider responseProvider;
    private final SafetyResponseTemplateExecutor templateExecutor;
    private final SafetyEventService safetyEventService;
    private final ChatAnalysisPipelineService analysisPipelineService;

    public ConversationTurnService(
            ConversationMessageService messageService,
            ConversationResponseProvider responseProvider,
            SafetyResponseTemplateExecutor templateExecutor,
            SafetyEventService safetyEventService,
            ChatAnalysisPipelineService analysisPipelineService) {
        this.messageService = messageService;
        this.responseProvider = responseProvider;
        this.templateExecutor = templateExecutor;
        this.safetyEventService = safetyEventService;
        this.analysisPipelineService = analysisPipelineService;
    }

    public ChatTurnResponse sendTurn(UUID sessionId, String content) {
        ConversationMessageService.MessageProcessingResult processed =
                messageService.processUserMessage(sessionId, content);
        ChatMessageResponse userMessage = processed.userMessage();

        if (processed.safety().status()
                == ConversationMessageService.SafetyEvaluationStatus.CONSENT_REQUIRED) {
            return ChatTurnResponse.of(userMessage, null, ReplyStatus.CONSENT_REQUIRED);
        }
        if (processed.safety().status()
                == ConversationMessageService.SafetyEvaluationStatus.FAILED) {
            return ChatTurnResponse.of(userMessage, null, ReplyStatus.SAFETY_UNAVAILABLE);
        }

        ResolverDecision decision = processed.safety().decision();
        if (decision.finalRiskLevel() >= SafetyEventService.BLOCKING_THRESHOLD) {
            return fixedSafetyResponse(userMessage, processed.safety());
        }

        UUID userId = messageService.getCurrentUserId();
        ChatMessageResponse.AnalysisStatus analysisStatus;
        try {
            analysisStatus = analysisPipelineService.analyze(
                    userMessage.id(), userId, userMessage.content());
        } catch (RuntimeException ex) {
            // Analysis is an auditable enrichment path. A failure must not
            // discard the already-saved user message or suppress a supportive
            // conversational response for a non-blocking Safety decision.
            analysisStatus = ChatMessageResponse.AnalysisStatus.FAILED;
            log.warn("Structured chat analysis unavailable for messageId={} causeClass={}",
                    userMessage.id(), ex.getClass().getSimpleName());
        }
        userMessage = withAnalysisStatus(userMessage, analysisStatus);

        try {
            String reply = responseProvider.generate(new ConversationResponseInput(
                    userId,
                    sessionId,
                    messageService.recentHistory(sessionId)));
            ChatMessageResponse assistant = messageService.saveAssistantMessage(
                    sessionId, userId, reply);
            return ChatTurnResponse.of(userMessage, assistant, ReplyStatus.SUCCEEDED);
        } catch (ProviderUnavailableException
                 | ProviderTimeoutException
                 | InvalidAnalysisOutputException ex) {
            log.warn("Conversation response unavailable for sessionId={} messageId={} code={} detail={}",
                    sessionId, userMessage.id(), ex.getCode().getCode(), ex.getMessage());
            return ChatTurnResponse.of(userMessage, null, ReplyStatus.PROVIDER_UNAVAILABLE);
        }
    }

    private ChatMessageResponse withAnalysisStatus(
            ChatMessageResponse message,
            ChatMessageResponse.AnalysisStatus status) {
        return new ChatMessageResponse(
                message.id(), message.sessionId(), message.role(), message.content(),
                message.createdAt(), status);
    }

    private ChatTurnResponse fixedSafetyResponse(
            ChatMessageResponse userMessage,
            ConversationMessageService.SafetyEvaluation safety) {
        UUID safetyEventId = safety.safetyEventId();
        String[] reasonCodes = safety.decision().reasonCodes();
        if (safetyEventId == null || reasonCodes == null || reasonCodes.length == 0) {
            return ChatTurnResponse.of(
                    userMessage, null, ReplyStatus.SAFETY_TEMPLATE_MISSING);
        }

        ResolvedResponse resolved = templateExecutor.resolve("vi", reasonCodes[0]);
        if (!resolved.isFound()) {
            safetyEventService.markShowTemplateSkipped(
                    safetyEventId, "No approved Safety response template configured");
            return ChatTurnResponse.of(
                    userMessage, null, ReplyStatus.SAFETY_TEMPLATE_MISSING);
        }

        UUID userId = messageService.getCurrentUserId();
        ChatMessageResponse assistant = messageService.saveAssistantMessage(
                userMessage.sessionId(), userId, resolved.getContent());
        safetyEventService.markShowTemplateSucceeded(
                safetyEventId, resolved.getTemplateId(), resolved.getTemplateVersion());
        return ChatTurnResponse.of(userMessage, assistant, ReplyStatus.SUCCEEDED);
    }
}
