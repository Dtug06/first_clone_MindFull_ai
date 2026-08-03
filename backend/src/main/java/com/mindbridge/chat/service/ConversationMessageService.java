package com.mindbridge.chat.service;

import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.service.BehavioralEventService;
import com.mindbridge.chat.domain.ChatSession;
import com.mindbridge.chat.domain.ConversationMessage;
import com.mindbridge.chat.domain.ChatSessionStatus;
import com.mindbridge.chat.dto.ChatMessageResponse;
import com.mindbridge.chat.exception.ChatSessionClosedException;
import com.mindbridge.chat.mapper.ConversationMessageMapper;
import com.mindbridge.chat.repository.ChatSessionRepository;
import com.mindbridge.chat.repository.ConversationMessageRepository;
import com.mindbridge.common.dto.PageResponse;
import com.mindbridge.common.exception.AccessDeniedException;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.consent.service.ConsentGuard;
import com.mindbridge.devseed.SeedGuard;
import com.mindbridge.safety.classifier.RiskClassifierInput;
import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.classifier.RiskClassifierProvider;
import com.mindbridge.safety.dto.PreFilterInput;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.event.SafetyActionType;
import com.mindbridge.safety.event.SafetyEventSourceType;
import com.mindbridge.safety.event.dto.SafetyActionSpec;
import com.mindbridge.safety.event.dto.SafetyEventSourceSpec;
import com.mindbridge.safety.event.service.SafetyEventService;
import com.mindbridge.safety.resolver.RiskStateSourceType;
import com.mindbridge.safety.resolver.dto.ResolverDecision;
import com.mindbridge.safety.resolver.dto.ResolverInput;
import com.mindbridge.safety.resolver.service.SafetyResolverService;
import com.mindbridge.safety.service.SafetyPreFilterService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages conversation messages for the current authenticated user.
 *
 * Security: every operation verifies that the session belongs to the current user
 * before allowing message send or read. This prevents injecting messages into
 * another user's session by forging a session_id.
 *
 * All userId values come from CurrentUserService (JWT principal).
 */
@Service
public class ConversationMessageService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMessageService.class);

    private final ConversationMessageRepository messageRepository;
    private final ChatSessionRepository sessionRepository;
    private final ConversationMessageMapper mapper;
    private final CurrentUserService currentUserService;
    private final MessagePreprocessor preprocessor;
    private final BehavioralEventService behavioralEventService;
    private final SeedGuard seedGuard;

    // G3-T11 — Safety pipeline collaborators. Injected lazily so the
    // existing test suite that builds this service manually (without
    // @SpringBootTest) can still inject nulls / mocks as long as the
    // safety path is never reached. The chat integration test
    // (ConversationMessageServiceSafetyIntegrationTest) boots a real
    // Spring context with these beans wired.
    private final ConsentGuard consentGuard;
    private final SafetyPreFilterService preFilterService;
    private final RiskClassifierProvider riskClassifierProvider;
    private final SafetyResolverService resolverService;
    private final SafetyEventService safetyEventService;

    public ConversationMessageService(
            ConversationMessageRepository messageRepository,
            ChatSessionRepository sessionRepository,
            ConversationMessageMapper mapper,
            CurrentUserService currentUserService,
            MessagePreprocessor preprocessor,
            BehavioralEventService behavioralEventService,
            SeedGuard seedGuard,
            ConsentGuard consentGuard,
            SafetyPreFilterService preFilterService,
            RiskClassifierProvider riskClassifierProvider,
            SafetyResolverService resolverService,
            SafetyEventService safetyEventService) {
        this.messageRepository = messageRepository;
        this.sessionRepository = sessionRepository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.preprocessor = preprocessor;
        this.behavioralEventService = behavioralEventService;
        this.seedGuard = seedGuard;
        this.consentGuard = consentGuard;
        this.preFilterService = preFilterService;
        this.riskClassifierProvider = riskClassifierProvider;
        this.resolverService = resolverService;
        this.safetyEventService = safetyEventService;
    }

    /**
     * Verifies the current user owns the given session.
     * Throws ResourceNotFoundException (404) if session doesn't exist.
     * Throws AccessDeniedException (403) if session belongs to another user.
     */
    private ChatSession requireSessionAccess(UUID sessionId) {
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
        currentUserService.verifyOwnership(session.getUserId());
        return session;
    }

    /**
     * Returns the current authenticated user id. Used by callers (e.g. the
     * IdempotencyService replay path) that need userId without invoking the
     * full business logic.
     */
    public java.util.UUID getCurrentUserId() {
        return currentUserService.getCurrentUserId();
    }

    /**
     * Sends a message to a chat session.
     * Validates and redacts the content via MessagePreprocessor before storage.
     * Verifies session ownership before saving.
     *
     * @throws ResourceNotFoundException  if session does not exist
     * @throws AccessDeniedException     if session belongs to another user
     * @throws ChatSessionClosedException if session is closed
     * @throws MessageValidationException if content is empty or exceeds length limit
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID sessionId, String content) {
        ChatSession session = requireSessionAccess(sessionId);

        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new ChatSessionClosedException(sessionId);
        }

        // Validate and redact — raw content is never logged or stored unredacted.
        String processedContent = preprocessor.process(content);
        boolean hasRedacted = preprocessor.isRedacted(processedContent);

        UUID userId = currentUserService.getCurrentUserId();
        ConversationMessage message = ConversationMessage.createUserMessage(
                sessionId, userId, processedContent, hasRedacted);
        ConversationMessage saved = messageRepository.save(message);

        // G3-T11 — Safety evaluation. Runs INSIDE this @Transactional
        // method so the resolver insert into risk_state_history and
        // the subsequent safety_events insert share the same
        // transaction (per Phase 1 decision C8). On any failure
        // inside the safety pipeline the raw message is still saved
        // (AI failure must not lose the raw message —
        // docs/01_ARCHITECTURE.md §7).
        evaluateSafetyPipeline(saved, userId, processedContent);

        // G2-T07: emit CHAT_MESSAGE_SENT event. Properties NEVER include raw
        // content — only length, role, redaction flag (see G2-T07 plan §2.3).
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("message_length", processedContent.length());
        props.put("role", "USER");
        props.put("was_redacted", hasRedacted);
        behavioralEventService.record(
                userId,
                BehavioralEventType.CHAT_MESSAGE_SENT,
                SourceType.CONVERSATION_MESSAGE,
                saved.getId(),
                props);
        return mapper.toResponse(saved);
    }

    /**
     * G3-T11 — Evaluate the safety pipeline (pre-filter → risk
     * classifier → resolver → safety event) for a freshly-saved user
     * message. Runs only when the user has granted {@code CHAT_ANALYSIS}
     * consent (per docs/04 §6 + T06 Phase 1 §Q3 "consent at T11 caller").
     *
     * <p>When the resolved risk level is L3 or L4, a {@code SafetyEvent}
     * is recorded with source {@code (CHAT_ANALYSIS, messageId)} and two
     * PENDING actions: {@code BLOCK_MATCHING} and {@code FLAG_REVIEW}.
     *
     * <p>Any exception thrown by the safety pipeline is caught and
     * logged at WARN with the message id only — the raw message MUST
     * NOT be lost (docs/01 §7). Failures bubble up only when they
     * affect the raw-message persistence (i.e. they don't).
     */
    private void evaluateSafetyPipeline(
            ConversationMessage saved, UUID userId, String processedContent) {
        if (!consentGuard.hasChatAnalysisConsent(userId)) {
            return;
        }
        try {
            PreFilterResult preFilter = preFilterService.evaluate(
                    new PreFilterInput(saved.getId(), userId, processedContent,
                            PreFilterInput.DEFAULT_LOCALE));
            RiskClassifierOutput classifier = riskClassifierProvider.classify(
                    new RiskClassifierInput(saved.getId(), userId, processedContent,
                            RiskClassifierInput.DEFAULT_LOCALE));
            ResolverDecision decision = resolverService.resolve(new ResolverInput(
                    userId,
                    RiskStateSourceType.LLM_CLASSIFIER,
                    saved.getId(),
                    preFilter,
                    classifier));

            if (decision.finalRiskLevel() >= SafetyEventService.BLOCKING_THRESHOLD) {
                safetyEventService.recordLevel3Or4Event(
                        decision,
                        List.of(new SafetyEventSourceSpec(
                                SafetyEventSourceType.CHAT_ANALYSIS, saved.getId())),
                        List.of(
                                new SafetyActionSpec(SafetyActionType.BLOCK_MATCHING),
                                new SafetyActionSpec(SafetyActionType.FLAG_REVIEW)));
            }
        } catch (RuntimeException ex) {
            // Log warn only — message id, exception class, no raw content.
            log.warn("Safety pipeline failed for messageId={} cause={}",
                    saved.getId(), ex.toString());
        }
    }

    /**
     * SEED-ONLY entry point (G2-T09). Sends a user-authored message into the
     * given session on behalf of the given user id. Used by the deterministic
     * dev seed in {@code com.mindbridge.devseed.DevSeedService}. Not for
     * production code paths — production code must use
     * {@link #sendMessage(UUID, String)} which derives the userId from the
     * JWT principal via {@code CurrentUserService} and verifies session
     * ownership.
     *
     * <p>Public only because Java has no package-friend mechanism across
     * packages; the {@code *ForSeed} suffix and explicit javadoc mark its
     * purpose. The {@code @ConditionalOnProperty} gate on
     * {@code DevSeedRunner} ensures the seed code path is opt-in.
     */
    public ChatMessageResponse sendMessageForSeed(UUID userId, UUID sessionId, String content) {
        seedGuard.requireSeedAllowed();
        ChatSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));

        if (session.getStatus() == ChatSessionStatus.CLOSED) {
            throw new ChatSessionClosedException(sessionId);
        }

        String processedContent = preprocessor.process(content);
        boolean hasRedacted = preprocessor.isRedacted(processedContent);

        ConversationMessage message = ConversationMessage.createUserMessage(
                sessionId, userId, processedContent, hasRedacted);
        ConversationMessage saved = messageRepository.save(message);

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("message_length", processedContent.length());
        props.put("role", "USER");
        props.put("was_redacted", hasRedacted);
        behavioralEventService.record(
                userId,
                BehavioralEventType.CHAT_MESSAGE_SENT,
                SourceType.CONVERSATION_MESSAGE,
                saved.getId(),
                props);
        return mapper.toResponse(saved);
    }

    /**
     * Returns a paginated list of messages for a session,
     * ordered by created_at ASC (oldest first).
     * Verifies session ownership before returning messages.
     *
     * @throws ResourceNotFoundException if session does not exist
     * @throws AccessDeniedException    if session belongs to another user
     */
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> listMessages(UUID sessionId, int page, int size) {
        requireSessionAccess(sessionId); // ownership check before reading messages

        Pageable pageable = PageRequest.of(page, size);
        Page<ConversationMessage> pageResult =
                messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);

        return new PageResponse<>(
                pageResult.getContent().stream().map(mapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
