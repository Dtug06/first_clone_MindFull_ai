package com.mindbridge.safety.classifier.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a {@code com.mindbridge.safety.classifier.RiskClassifierProvider}
 * returns a payload that fails schema validation. Per
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §7 the call must NOT be
 * persisted as a successful run when this is thrown — the consuming
 * service is responsible for marking the AI run as {@code FAILED} and
 * routing the failure path.
 *
 * <p>The exception message must NOT include the raw user message
 * content or the malformed payload itself.
 */
public class InvalidRiskClassifierOutputException extends MindBridgeException {

    public InvalidRiskClassifierOutputException(String message) {
        super(ErrorCode.RISK_CLASSIFIER_OUTPUT_INVALID, message);
    }

    public InvalidRiskClassifierOutputException() {
        super(ErrorCode.RISK_CLASSIFIER_OUTPUT_INVALID);
    }
}
