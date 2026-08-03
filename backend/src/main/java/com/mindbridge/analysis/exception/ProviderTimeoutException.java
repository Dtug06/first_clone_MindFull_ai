package com.mindbridge.analysis.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a {@link com.mindbridge.analysis.provider.ChatAnalysisProvider}
 * exceeds its timeout while analysing a message. Maps to HTTP 502 Bad
 * Gateway via {@code GlobalExceptionHandler} (treated as an upstream
 * failure, not an internal server bug).
 *
 * <p>The exception message must NOT include the raw user message content.
 */
public class ProviderTimeoutException extends MindBridgeException {

    public ProviderTimeoutException(String message) {
        super(ErrorCode.AI_PROVIDER_TIMEOUT, message);
    }

    public ProviderTimeoutException() {
        super(ErrorCode.AI_PROVIDER_TIMEOUT);
    }
}