package com.mindbridge.auth.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a registration attempt uses an email address that is already
 * registered. Maps to HTTP 409 Conflict.
 */
public class DuplicateEmailException extends MindBridgeException {

    public DuplicateEmailException(String email) {
        super(ErrorCode.USER_EMAIL_DUPLICATE,
                "An account with email '" + email + "' already exists");
    }
}
