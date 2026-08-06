package com.mindbridge.safety.resolver.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when {@code SafetyResolverService.resolve(...)} is called
 * with an invalid {@code ResolverInput}. Caught by
 * {@code GlobalExceptionHandler} and mapped to
 * {@code HttpStatus.BAD_REQUEST} via the existing
 * {@link ErrorCode#VALIDATION_ERROR} code (no new error code added —
 * same pattern as G3-T08's {@code SafetyPreFilterInputException}).
 */
public class SafetyResolverInputException extends MindBridgeException {

    public SafetyResolverInputException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
