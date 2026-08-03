package com.mindbridge.chat.service;

import com.mindbridge.behavior.domain.BehavioralEventType;
import com.mindbridge.behavior.domain.SourceType;
import com.mindbridge.behavior.service.BehavioralEventService;
import com.mindbridge.chat.domain.ChatSession;
import com.mindbridge.chat.dto.ChatSessionResponse;
import com.mindbridge.chat.mapper.ChatSessionMapper;
import com.mindbridge.chat.repository.ChatSessionRepository;
import com.mindbridge.common.dto.PageResponse;
import com.mindbridge.common.exception.AccessDeniedException;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.devseed.SeedGuard;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages chat sessions for the current authenticated user.
 *
 * All userId values come from CurrentUserService (JWT principal).
 * Ownership is verified before returning or modifying any session.
 */
@Service
public class ChatSessionService {

    private final ChatSessionRepository repository;
    private final ChatSessionMapper mapper;
    private final CurrentUserService currentUserService;
    private final BehavioralEventService behavioralEventService;
    private final SeedGuard seedGuard;

    public ChatSessionService(ChatSessionRepository repository,
                             ChatSessionMapper mapper,
                             CurrentUserService currentUserService,
                             BehavioralEventService behavioralEventService,
                             SeedGuard seedGuard) {
        this.repository = repository;
        this.mapper = mapper;
        this.currentUserService = currentUserService;
        this.behavioralEventService = behavioralEventService;
        this.seedGuard = seedGuard;
    }

    /**
     * Creates a new ACTIVE chat session for the current user.
     * Title is optional.
     */
    @Transactional
    public ChatSessionResponse createSession(String title) {
        UUID userId = currentUserService.getCurrentUserId();
        ChatSession session = ChatSession.create(userId, title);
        ChatSession saved = repository.save(session);
        // G2-T07: emit CHAT_SESSION_STARTED event.
        // Properties: only "title_present" flag (boolean) — never the raw title
        // because title may contain user-provided text (potential PII).
        behavioralEventService.record(
                userId,
                BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION,
                saved.getId(),
                Map.of("title_present", title != null && !title.isBlank()));
        return mapper.toResponse(saved);
    }

    /**
     * Returns a paginated list of the current user's sessions,
     * ordered by most recently active (updated_at DESC).
     */
    @Transactional(readOnly = true)
    public PageResponse<ChatSessionResponse> listSessions(int page, int size) {
        UUID userId = currentUserService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatSession> pageResult = repository.findByUserIdOrderByUpdatedAtDesc(userId, pageable);
        return new PageResponse<>(
                pageResult.getContent().stream().map(mapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    /**
     * Returns a single session by id, verifying the current user owns it.
     *
     * @throws ResourceNotFoundException if session does not exist
     * @throws AccessDeniedException    if session belongs to another user
     */
    @Transactional(readOnly = true)
    public ChatSessionResponse getSession(UUID sessionId) {
        ChatSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
        currentUserService.verifyOwnership(session.getUserId());
        return mapper.toResponse(session);
    }

    /**
     * SEED-ONLY entry point (G2-T09). Creates a new ACTIVE chat session for
     * the given user id. Used by the deterministic dev seed in
     * {@code com.mindbridge.devseed.DevSeedService}. Not for production
     * code paths — production code must use
     * {@link #createSession(String)} which derives the userId from the JWT
     * principal via {@code CurrentUserService}.
     *
     * <p>Public only because Java has no package-friend mechanism across
     * packages; the {@code *ForSeed} suffix and explicit javadoc mark its
     * purpose. The {@code @ConditionalOnProperty} gate on
     * {@code DevSeedRunner} ensures the seed code path is opt-in.
     */
    public ChatSession createSessionForSeed(UUID userId, String title) {
        seedGuard.requireSeedAllowed();
        ChatSession session = ChatSession.create(userId, title);
        ChatSession saved = repository.save(session);
        behavioralEventService.record(
                userId,
                BehavioralEventType.CHAT_SESSION_STARTED,
                SourceType.CHAT_SESSION,
                saved.getId(),
                Map.of("title_present", title != null && !title.isBlank()));
        return saved;
    }

    /**
     * Closes an existing session. Verifies ownership before modification.
     *
     * @throws ResourceNotFoundException if session does not exist
     * @throws AccessDeniedException    if session belongs to another user
     */
    @Transactional
    public ChatSessionResponse closeSession(UUID sessionId) {
        ChatSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));
        currentUserService.verifyOwnership(session.getUserId());
        session.close();
        ChatSession saved = repository.save(session);
        return mapper.toResponse(saved);
    }
}
