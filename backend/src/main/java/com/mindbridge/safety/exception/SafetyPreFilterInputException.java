package com.mindbridge.safety.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown by the pre-filter when its input is structurally invalid
 * (null ids, blank or oversized content, etc.). The caller is expected
 * to map this to HTTP 400 via the shared
 * {@code GlobalExceptionHandler}.
 *
 * <p>This exception is for INPUT validation only — pre-filter business
 * outcomes (matched, not matched) are NOT expressed as exceptions.
 */
public class SafetyPreFilterInputException extends MindBridgeException {

    public SafetyPreFilterInputException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public SafetyPreFilterInputException() {
        super(ErrorCode.VALIDATION_ERROR);
    }
}
