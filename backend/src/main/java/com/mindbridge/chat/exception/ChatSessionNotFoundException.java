package com.mindbridge.chat.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a chat session cannot be found.
 * Maps to HTTP 404 via GlobalExceptionHandler.
 */
public class ChatSessionNotFoundException extends MindBridgeException {

    public ChatSessionNotFoundException(Object sessionId) {
        super(
                ErrorCode.CHAT_SESSION_NOT_FOUND,
                "Chat session with id '" + sessionId + "' not found"
        );
    }
}
