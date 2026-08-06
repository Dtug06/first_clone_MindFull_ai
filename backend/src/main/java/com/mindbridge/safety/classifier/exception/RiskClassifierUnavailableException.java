package com.mindbridge.safety.classifier.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when the risk classifier provider is unreachable, returns
 * 5xx, or rate-limits after retries are exhausted. Maps to HTTP 502
 * Bad Gateway via {@code GlobalExceptionHandler}.
 *
 * <p>The exception message must NOT include the raw user message
 * content or provider credentials.
 */
public class RiskClassifierUnavailableException extends MindBridgeException {

    public RiskClassifierUnavailableException(String message) {
        super(ErrorCode.RISK_CLASSIFIER_UNAVAILABLE, message);
    }

    public RiskClassifierUnavailableException() {
        super(ErrorCode.RISK_CLASSIFIER_UNAVAILABLE);
    }
}
