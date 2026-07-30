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
}
