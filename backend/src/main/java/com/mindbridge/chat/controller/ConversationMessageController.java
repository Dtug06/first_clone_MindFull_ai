package com.mindbridge.chat.controller;

import com.mindbridge.chat.dto.ChatMessageResponse;
import com.mindbridge.chat.dto.ChatTurnResponse;
import com.mindbridge.chat.dto.SendMessageRequest;
import com.mindbridge.chat.service.ConversationMessageService;
import com.mindbridge.chat.service.ConversationTurnService;
import com.mindbridge.common.dto.PageResponse;
import com.mindbridge.idempotency.service.IdempotencyService;
import com.mindbridge.idempotency.service.IdempotencyService.IdempotencyResult;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conversation message controller.
 *
 * All operations verify session ownership via ConversationMessageService.
 * Client-supplied userId is never trusted.
 *
 * G2-T08: POST /messages accepts an optional {@code Idempotency-Key} header.
 * Same key + same payload = same response (replay). Missing key = legacy behavior
 * (no idempotency, retry may create duplicate messages).
 */
@RestController
@RequestMapping("/chat/sessions/{sessionId}/messages")
public class ConversationMessageController {

    /** Logical endpoint identifier used as the idempotency key group. */
    static final String ENDPOINT = "POST:/chat/sessions/{sessionId}/messages";

    private final ConversationMessageService messageService;
    private final ConversationTurnService turnService;
    private final IdempotencyService idempotencyService;

    public ConversationMessageController(
            ConversationMessageService messageService,
            ConversationTurnService turnService,
            IdempotencyService idempotencyService) {
        this.messageService = messageService;
        this.turnService = turnService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * POST /chat/sessions/{sessionId}/messages — sends a message to the session.
     * Verifies session ownership before saving.
     *
     * @param idempotencyKey optional client-supplied key for retry-safe double-click
     */
    @PostMapping
    public ResponseEntity<ChatTurnResponse> sendMessage(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody SendMessageRequest request) {

        // Resolve userId from JWT principal — never trust client.
        // CurrentUserService is called inside the service, but we also need it
        // here for the idempotency key lookup. The lookup is by userId so it's
        // safe to do BEFORE the supplier runs (replay must not execute the
        // supplier at all).
        UUID userId = messageService.getCurrentUserId();

        IdempotencyResult<ChatTurnResponse> result = idempotencyService.executeWithIdempotency(
                userId,
                ENDPOINT,
                idempotencyKey,
                ChatTurnResponse.class,
                () -> IdempotencyService.result(
                        turnService.sendTurn(sessionId, request.content()),
                        HttpStatus.CREATED));

        return ResponseEntity.status(result.status()).body(result.body());
    }

    /**
     * GET /chat/sessions/{sessionId}/messages — returns paginated messages for the session.
     * Verifies session ownership before returning any message.
     */
    @GetMapping
    public ResponseEntity<PageResponse<ChatMessageResponse>> listMessages(
            @PathVariable UUID sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(messageService.listMessages(sessionId, page, size));
    }
}
