package com.mindbridge.chat.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when an operation is attempted on a closed chat session.
 * Maps to HTTP 409 Conflict via GlobalExceptionHandler.
 */
public class ChatSessionClosedException extends MindBridgeException {

    public ChatSessionClosedException(Object sessionId) {
        super(
                ErrorCode.CHAT_SESSION_CLOSED,
                "Chat session with id '" + sessionId + "' is closed and cannot accept new messages"
        );
    }
}
