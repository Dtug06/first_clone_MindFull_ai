package com.mindbridge.dailyquestion.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a daily question answer violates business rules.
 *
 * Subclass of MindBridgeException so the GlobalExceptionHandler picks it up.
 */
public class AnswerBusinessException extends MindBridgeException {

    public AnswerBusinessException(ErrorCode code, String message) {
        super(code, message);
    }
}