package com.mindbridge.safety.event.exception;

public class SafetyEventInputException extends RuntimeException {

    public SafetyEventInputException(String message) {
        super(message);
    }

    public SafetyEventInputException(String message, Throwable cause) {
        super(message, cause);
    }
}