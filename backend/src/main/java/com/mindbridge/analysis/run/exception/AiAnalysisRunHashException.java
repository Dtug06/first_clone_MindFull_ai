package com.mindbridge.analysis.run.exception;

import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when the AI analysis run row cannot be created or updated
 * because of a hash-computation failure (e.g. MessageDigest algorithm
 * not available). Maps to {@code HttpStatus.INTERNAL_SERVER_ERROR}
 * via {@code GlobalExceptionHandler} (which already maps the
 * INTERNAL_ERROR code → 500).
 *
 * <p>In practice this should never happen — {@code SHA-256} is a
 * JDK-mandated algorithm. The exception exists for defensive
 * boundaries at the service entry point.
 */
public class AiAnalysisRunHashException extends MindBridgeException {

    /**
     * Wrap the underlying cause. The cause's message is NOT
     * exposed to the caller — the human-readable message is fixed
     * so it cannot accidentally leak crypto internals.
     */
    public AiAnalysisRunHashException(Throwable cause) {
        super(com.mindbridge.common.exception.ErrorCode.INTERNAL_ERROR,
                "Failed to compute hash for AI analysis run");
        initCause(cause);
    }
}