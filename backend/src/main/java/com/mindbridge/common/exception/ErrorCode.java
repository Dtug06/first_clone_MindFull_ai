package com.mindbridge.common.exception;

/**
 * Stable error codes used in API ErrorResponse objects.
 * Each entry has a unique string code and a default message.
 *
 * Codes follow the pattern: DOMAIN_SHORTNAME_MEANINGFUL_NAME
 * (e.g. USER_EMAIL_DUPLICATE, AUTH_CREDENTIALS_INVALID)
 */
public enum ErrorCode {

    // --- Generic ---
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "The requested resource was not found"),
    VALIDATION_ERROR("VALIDATION_ERROR", "One or more fields failed validation"),
    INTERNAL_ERROR("INTERNAL_ERROR", "An unexpected error occurred"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED", "HTTP method not allowed for this endpoint"),
    MEDIA_TYPE_NOT_ACCEPTABLE("MEDIA_TYPE_NOT_ACCEPTABLE", "Requested media type is not supported"),

    // --- Auth / User ---
    AUTH_CREDENTIALS_INVALID("AUTH_CREDENTIALS_INVALID", "Email or password is incorrect"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", "Access token has expired"),
    AUTH_TOKEN_INVALID("AUTH_TOKEN_INVALID", "Access token is invalid"),
    USER_EMAIL_DUPLICATE("USER_EMAIL_DUPLICATE", "An account with this email already exists"),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found"),
    USER_SUSPENDED("USER_SUSPENDED", "This account has been suspended"),

    // --- Authorization ---
    ACCESS_DENIED("ACCESS_DENIED", "You do not have permission to access this resource"),

    // --- Consent ---
    CONSENT_TYPE_NOT_FOUND("CONSENT_TYPE_NOT_FOUND", "Unknown consent type"),
    CONSENT_REVOKED("CONSENT_REVOKED", "Required consent has been revoked"),

    // --- Chat ---
    CHAT_SESSION_NOT_FOUND("CHAT_SESSION_NOT_FOUND", "Chat session not found"),
    CHAT_SESSION_ACCESS_DENIED("CHAT_SESSION_ACCESS_DENIED", "You do not have access to this chat session"),
    CHAT_SESSION_CLOSED("CHAT_SESSION_CLOSED", "Chat session is closed and cannot accept new messages"),

    // --- Daily Check-in ---
    CHECKIN_ANSWER_DUPLICATE("CHECKIN_ANSWER_DUPLICATE", "Answer already submitted for this assignment today"),
    CHECKIN_ASSIGNMENT_NOT_FOUND("CHECKIN_ASSIGNMENT_NOT_FOUND", "Check-in assignment not found"),

    // --- CBT / Matching ---
    PROGRAM_NOT_FOUND("PROGRAM_NOT_FOUND", "CBT program not found"),
    USER_PROGRAM_NOT_FOUND("USER_PROGRAM_NOT_FOUND", "User program not found"),
    MATCHING_INSUFFICIENT_DATA("MATCHING_INSUFFICIENT_DATA", "Not enough data to run program matching"),
    MATCHING_SAFETY_BLOCKED("MATCHING_SAFETY_BLOCKED", "Program matching is blocked due to safety concerns"),
    MATCHING_NO_CANDIDATE("MATCHING_NO_CANDIDATE", "No matching program found for this user"),

    // --- Behavior ---
    BEHAVIOR_PROFILE_NOT_FOUND("BEHAVIOR_PROFILE_NOT_FOUND", "Behavior profile not found"),

    // --- Consent ---
    CONSENT_REQUIRED("CONSENT_REQUIRED", "Required consent has not been granted");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
