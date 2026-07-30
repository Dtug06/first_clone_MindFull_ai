package com.mindbridge.consent.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when an action requires a consent the user has not granted or has revoked.
 *
 * Maps to HTTP 409 Conflict (per 03_API_CONTRACT.yaml semantics).
 */
public class ConsentRequiredException extends MindBridgeException {

    public ConsentRequiredException(String message) {
        super(ErrorCode.CONSENT_REQUIRED, message);
    }
}