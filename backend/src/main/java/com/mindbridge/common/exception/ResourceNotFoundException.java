package com.mindbridge.common.exception;

/**
 * Runtime exception thrown when a requested resource cannot be found.
 * Maps to HTTP 404 / NotFound response in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends MindBridgeException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
                ErrorCode.RESOURCE_NOT_FOUND,
                resourceName + " with id '" + identifier + "' not found"
        );
    }

    /**
     * Caller-supplied error code (e.g. {@code BEHAVIOR_PROFILE_NOT_FOUND})
     * and a custom message. Used when the default RESOURCE_NOT_FOUND code
     * is too generic.
     */
    public ResourceNotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}
