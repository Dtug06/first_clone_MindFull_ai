package com.mindbridge.safety.response.exception;

/**
 * Thrown by {@code SafetyResponseTemplate} (factory / transitions) and
 * {@code SafetyResponseTemplateService} when an input argument violates
 * the documented contract (null id, blank code, malformed version,
 * invalid locale, invalid risk_reason, status transition from the wrong
 * starting state, approving with a non-EXPERT/ADMIN user, etc.).
 *
 * <p>Maps to HTTP 400 Bad Request at the controller layer once an admin
 * endpoint is added in a future task. Internal callers catch and log
 * without propagating; admin tooling surfaces the message.
 *
 * <p>Mirrors the style of {@code SafetyKeywordRuleException} family and
 * {@code SafetyEventInputException} (T11): one bare exception class,
 * caught by the project's {@code GlobalExceptionHandler}.
 */
public class SafetyResponseTemplateInputException extends RuntimeException {

    public SafetyResponseTemplateInputException(String message) {
        super(message);
    }
}
