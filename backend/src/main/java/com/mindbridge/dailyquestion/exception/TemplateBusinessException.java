package com.mindbridge.dailyquestion.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a template or operation violates business rules.
 */
public class TemplateBusinessException extends MindBridgeException {

    public TemplateBusinessException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
