package com.mindbridge.common.exception;

/**
 * Thrown when an authenticated user attempts to access a resource they do not own,
 * or lacks the required role for the requested operation.
 *
 * Maps to HTTP 403 Forbidden.
 */
public class AccessDeniedException extends MindBridgeException {

    public AccessDeniedException() {
        super(ErrorCode.ACCESS_DENIED);
    }

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }
}
