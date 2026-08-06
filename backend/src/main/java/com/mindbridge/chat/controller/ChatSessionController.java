package com.mindbridge.chat.controller;

import com.mindbridge.chat.dto.ChatSessionResponse;
import com.mindbridge.chat.dto.CreateChatSessionRequest;
import com.mindbridge.chat.service.ChatSessionService;
import com.mindbridge.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chat session controller.
 *
 * userId is always taken from the JWT principal (ChatSessionService → CurrentUserService).
 * Client-supplied userId is never trusted.
 */
@RestController
@RequestMapping("/chat/sessions")
public class ChatSessionController {

    private final ChatSessionService service;

    public ChatSessionController(ChatSessionService service) {
        this.service = service;
    }

    /**
     * POST /chat/sessions — creates a new chat session.
     * Request body is optional (title may be null).
     */
    @PostMapping
    public ResponseEntity<ChatSessionResponse> createSession(
            @Valid @RequestBody(required = false) CreateChatSessionRequest request) {
        String title = (request != null) ? request.title() : null;
        ChatSessionResponse response = service.createSession(title);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /chat/sessions — returns the current user's sessions ordered by most recently active.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ChatSessionResponse>> listSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.listSessions(page, size));
    }

    /**
     * GET /chat/sessions/{sessionId} — returns a single session by id.
     * Returns 404 if not found, 403 if owned by another user.
     */
    @GetMapping("/{sessionId}")
    public ResponseEntity<ChatSessionResponse> getSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(service.getSession(sessionId));
    }

    /**
     * POST /chat/sessions/{sessionId}/close — closes an active session.
     * Returns 404 if not found, 403 if owned by another user.
     */
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<ChatSessionResponse> closeSession(@PathVariable UUID sessionId) {
        return ResponseEntity.ok(service.closeSession(sessionId));
    }
}
