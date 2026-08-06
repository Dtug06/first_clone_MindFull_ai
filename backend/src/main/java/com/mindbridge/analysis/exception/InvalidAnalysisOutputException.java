package com.mindbridge.analysis.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a {@link com.mindbridge.analysis.provider.ChatAnalysisProvider}
 * returns a payload that fails schema validation. Per
 * {@code docs/01_ARCHITECTURE.md} §8 the call must NOT be persisted as
 * a successful run when this is thrown — the consuming service is
 * responsible for marking the AI run as {@code FAILED} and routing the
 * failure path.
 *
 * <p>The exception message must NOT include the raw user message content
 * or the malformed payload itself.
 */
public class InvalidAnalysisOutputException extends MindBridgeException {

    public InvalidAnalysisOutputException(String message) {
        super(ErrorCode.AI_ANALYSIS_OUTPUT_INVALID, message);
    }

    public InvalidAnalysisOutputException() {
        super(ErrorCode.AI_ANALYSIS_OUTPUT_INVALID);
    }
}