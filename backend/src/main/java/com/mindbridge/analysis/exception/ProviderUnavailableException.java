package com.mindbridge.analysis.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when the AI provider is unreachable, returns 5xx, or rate-limits
 * after retries are exhausted. Maps to HTTP 502 Bad Gateway via
 * {@code GlobalExceptionHandler}.
 *
 * <p>The exception message must NOT include the raw user message content
 * or provider credentials.
 */
public class ProviderUnavailableException extends MindBridgeException {

    public ProviderUnavailableException(String message) {
        super(ErrorCode.AI_PROVIDER_UNAVAILABLE, message);
    }

    public ProviderUnavailableException() {
        super(ErrorCode.AI_PROVIDER_UNAVAILABLE);
    }
}