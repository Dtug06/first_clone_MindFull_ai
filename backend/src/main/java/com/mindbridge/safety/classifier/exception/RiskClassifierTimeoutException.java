package com.mindbridge.safety.classifier.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a {@code com.mindbridge.safety.classifier.RiskClassifierProvider}
 * exceeds its timeout while classifying a message. Maps to HTTP 502
 * Bad Gateway via {@code GlobalExceptionHandler} (treated as an
 * upstream failure, not an internal server bug).
 *
 * <p>The exception message must NOT include the raw user message
 * content.
 */
public class RiskClassifierTimeoutException extends MindBridgeException {

    public RiskClassifierTimeoutException(String message) {
        super(ErrorCode.RISK_CLASSIFIER_TIMEOUT, message);
    }

    public RiskClassifierTimeoutException() {
        super(ErrorCode.RISK_CLASSIFIER_TIMEOUT);
    }
}
