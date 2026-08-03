package com.mindbridge.chat.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when message content fails preprocessing validation.
 * Maps to HTTP 400 Bad Request via GlobalExceptionHandler.
 *
 * The exception message is intentionally generic — never includes raw content.
 */
public class MessageValidationException extends MindBridgeException {

    public MessageValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
