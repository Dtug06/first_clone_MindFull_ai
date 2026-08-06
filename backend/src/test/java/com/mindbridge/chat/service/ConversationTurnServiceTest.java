package com.mindbridge.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mindbridge.chat.ai.ConversationResponseInput;
import com.mindbridge.chat.ai.ConversationResponseProvider;
import com.mindbridge.analysis.pipeline.ChatAnalysisPipelineService;
import com.mindbridge.chat.dto.ChatMessageResponse;
import com.mindbridge.chat.dto.ChatTurnResponse;
import com.mindbridge.safety.event.service.SafetyEventService;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.response.executor.SafetyResponseTemplateExecutor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationTurnServiceTest {

    @Mock
    private ConversationMessageService messageService;
    @Mock
    private ConversationResponseProvider responseProvider;
    @Mock
    private SafetyResponseTemplateExecutor templateExecutor;
    @Mock
    private SafetyEventService safetyEventService;
    @Mock
    private ChatAnalysisPipelineService analysisPipelineService;

    private ConversationTurnService service;
    private UUID userId;
    private UUID sessionId;
    private ChatMessageResponse userMessage;

    @BeforeEach
    void setUp() {
        service = new ConversationTurnService(
                messageService, responseProvider, templateExecutor, safetyEventService,
                analysisPipelineService);
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        userMessage = new ChatMessageResponse(
                UUID.randomUUID(), sessionId,
                ChatMessageResponse.MessageRole.USER,
                "hello", Instant.now(),
                ChatMessageResponse.AnalysisStatus.SUCCEEDED);
    }

    @Test
    void consentMissingStoresUserButDoesNotCallAnyProvider() {
        when(messageService.processUserMessage(sessionId, "hello"))
                .thenReturn(new ConversationMessageService.MessageProcessingResult(
                        userMessage,
                        new ConversationMessageService.SafetyEvaluation(
                                ConversationMessageService.SafetyEvaluationStatus.CONSENT_REQUIRED,
                                null, null)));

        ChatTurnResponse result = service.sendTurn(sessionId, "hello");

        assertThat(result.replyStatus())
                .isEqualTo(ChatTurnResponse.ReplyStatus.CONSENT_REQUIRED);
        assertThat(result.assistantMessage()).isNull();
        verifyNoInteractions(responseProvider, templateExecutor, analysisPipelineService);
    }

    @Test
    void levelOneCallsConversationProviderAndPersistsAssistant() {
        ResolverDecision decision = decision((short) 1, "MAX_WINS_L1");
        when(messageService.processUserMessage(sessionId, "hello"))
                .thenReturn(processed(decision, null));
        when(messageService.getCurrentUserId()).thenReturn(userId);
        when(analysisPipelineService.analyze(userMessage.id(), userId, userMessage.content()))
                .thenReturn(ChatMessageResponse.AnalysisStatus.SUCCEEDED);
        when(messageService.recentHistory(sessionId)).thenReturn(List.of(
                new ConversationResponseInput.HistoryMessage("user", "hello")));
        when(responseProvider.generate(any(ConversationResponseInput.class)))
                .thenReturn("supportive reply");
        ChatMessageResponse assistant = new ChatMessageResponse(
                UUID.randomUUID(), sessionId,
                ChatMessageResponse.MessageRole.ASSISTANT,
                "supportive reply", Instant.now(),
                ChatMessageResponse.AnalysisStatus.NOT_REQUESTED);
        when(messageService.saveAssistantMessage(sessionId, userId, "supportive reply"))
                .thenReturn(assistant);

        ChatTurnResponse result = service.sendTurn(sessionId, "hello");

        assertThat(result.replyStatus()).isEqualTo(ChatTurnResponse.ReplyStatus.SUCCEEDED);
        assertThat(result.assistantMessage()).isEqualTo(assistant);
        verifyNoInteractions(templateExecutor);
        verify(analysisPipelineService).analyze(
                userMessage.id(), userId, userMessage.content());
    }

    @Test
    void structuredAnalysisFailureDoesNotSuppressConversationReply() {
        ResolverDecision decision = decision((short) 1, "MAX_WINS_L1");
        when(messageService.processUserMessage(sessionId, "hello"))
                .thenReturn(processed(decision, null));
        when(messageService.getCurrentUserId()).thenReturn(userId);
        when(analysisPipelineService.analyze(userMessage.id(), userId, userMessage.content()))
                .thenThrow(new IllegalStateException("provider unavailable"));
        when(messageService.recentHistory(sessionId)).thenReturn(List.of(
                new ConversationResponseInput.HistoryMessage("user", "hello")));
        when(responseProvider.generate(any(ConversationResponseInput.class)))
                .thenReturn("supportive reply");
        ChatMessageResponse assistant = new ChatMessageResponse(
                UUID.randomUUID(), sessionId,
                ChatMessageResponse.MessageRole.ASSISTANT,
                "supportive reply", Instant.now(),
                ChatMessageResponse.AnalysisStatus.NOT_REQUESTED);
        when(messageService.saveAssistantMessage(sessionId, userId, "supportive reply"))
                .thenReturn(assistant);

        ChatTurnResponse result = service.sendTurn(sessionId, "hello");

        assertThat(result.replyStatus()).isEqualTo(ChatTurnResponse.ReplyStatus.SUCCEEDED);
        assertThat(result.analysisStatus()).isEqualTo(ChatMessageResponse.AnalysisStatus.FAILED);
        assertThat(result.assistantMessage()).isEqualTo(assistant);
    }

    @Test
    void levelFourNeverCallsFreeFormProviderWhenTemplateIsMissing() {
        UUID safetyEventId = UUID.randomUUID();
        ResolverDecision decision = decision((short) 4, "MAX_WINS_L4");
        when(messageService.processUserMessage(sessionId, "hello"))
                .thenReturn(processed(decision, safetyEventId));
        when(templateExecutor.resolve("vi", "MAX_WINS_L4"))
                .thenReturn(SafetyResponseTemplateExecutor.ResolvedResponse.empty());

        ChatTurnResponse result = service.sendTurn(sessionId, "hello");

        assertThat(result.replyStatus())
                .isEqualTo(ChatTurnResponse.ReplyStatus.SAFETY_TEMPLATE_MISSING);
        assertThat(result.assistantMessage()).isNull();
        verify(responseProvider, never()).generate(any());
        verifyNoInteractions(analysisPipelineService);
        verify(safetyEventService).markShowTemplateSkipped(
                safetyEventId, "No approved Safety response template configured");
    }

    private ConversationMessageService.MessageProcessingResult processed(
            ResolverDecision decision, UUID safetyEventId) {
        return new ConversationMessageService.MessageProcessingResult(
                userMessage,
                new ConversationMessageService.SafetyEvaluation(
                        ConversationMessageService.SafetyEvaluationStatus.EVALUATED,
                        decision,
                        safetyEventId));
    }

    private ResolverDecision decision(short level, String reasonCode) {
        return new ResolverDecision(
                level, level, (short) 1, null,
                BigDecimal.valueOf(0.9),
                new String[]{reasonCode},
                null);
    }
}
