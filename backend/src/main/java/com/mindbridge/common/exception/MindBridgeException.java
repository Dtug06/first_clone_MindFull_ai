package com.mindbridge.common.exception;

/**
 * Base application exception that carries a stable error code (used in API
 * responses) and a human-readable message. Subclasses carry no mutable state
 * so instances are safe to reuse across request boundaries.
 */
public class MindBridgeException extends RuntimeException {

    private final ErrorCode code;

    public MindBridgeException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public MindBridgeException(ErrorCode code) {
        this(code, code.getDefaultMessage());
    }

    public ErrorCode getCode() {
        return code;
    }
}
