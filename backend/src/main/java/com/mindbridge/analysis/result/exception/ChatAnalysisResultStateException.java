package com.mindbridge.analysis.result.exception;

import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;

/**
 * Thrown when a state transition on a {@code chat_analysis_results} row
 * is rejected (e.g. attempting to mark a row as SUPERSEDED when it is
 * not ACTIVE, or the database trigger rejects the transition).
 */
public class ChatAnalysisResultStateException extends MindBridgeException {

    public ChatAnalysisResultStateException(String message) {
        super(ErrorCode.INTERNAL_ERROR, message);
    }

    public ChatAnalysisResultStateException(String message, Throwable cause) {
        super(ErrorCode.INTERNAL_ERROR, message);
        initCause(cause);
    }
}