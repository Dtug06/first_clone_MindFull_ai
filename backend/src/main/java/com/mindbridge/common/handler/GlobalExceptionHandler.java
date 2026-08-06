package com.mindbridge.common.handler;

import com.mindbridge.common.dto.ErrorResponse;
import com.mindbridge.common.dto.FieldError;
import com.mindbridge.common.exception.AccessDeniedException;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.util.RequestContext;
import com.mindbridge.safety.event.exception.SafetyEventInputException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralised exception-to-HTTP mapping. Every handler produces an ErrorResponse
 * matching the schema defined in 03_API_CONTRACT.yaml.
 *
 * Rules:
 * - Never leak internal error details (stack traces, SQL state) to the client.
 * - Log the full exception at WARN/ERROR so ops can debug.
 * - RequestId and path come from RequestContext (MDC), not the exception.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 4xx mapped exceptions ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                           HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                "Validation failed",
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null),
                fieldErrors
        );

        log.warn("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MindBridgeException.class)
    public ResponseEntity<ErrorResponse> handleMindBridge(MindBridgeException ex,
                                                           HttpServletRequest request) {
        HttpStatus status = statusFor(ex);
        ErrorResponse body = new ErrorResponse(
                ex.getCode().getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        if (status.is4xxClientError()) {
            log.warn("Client error on {}: {} — {}", request.getRequestURI(), ex.getCode().getCode(), ex.getMessage());
        } else {
            log.error("Server error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        }

        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String message = String.format("Parameter '%s' should be of type %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                message,
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
                                                               HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                "Request body is missing or malformed JSON",
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Unreadable body on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    // --- 404 / 405 / 406 ---

    @ExceptionHandler({NoResourceFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.RESOURCE_NOT_FOUND.getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Not found on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Method not allowed on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                       HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Media type not supported on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                       HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.MEDIA_TYPE_NOT_ACCEPTABLE.getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Media type not acceptable on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(body);
    }

    // --- G3-T13: Safety / Expert Review specific ---

    /**
     * Safety event input errors (event not found, invalid status transition, etc.).
     * Maps to HTTP 404 (not found) or 400 (bad request) depending on the message.
     */
    @ExceptionHandler(SafetyEventInputException.class)
    public ResponseEntity<ErrorResponse> handleSafetyEventInput(SafetyEventInputException ex,
                                                               HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ex.getMessage(),
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Safety event input error on {}: {}", request.getRequestURI(), ex.getMessage());

        // Not-found messages get 404; all others get 400
        HttpStatus status = ex.getMessage().contains("not found")
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Constraint violations (e.g. duplicate expert review submission).
     * Maps to HTTP 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(DataIntegrityViolationException ex,
                                                                  HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.VALIDATION_ERROR.getCode(),
                "Data conflict: the request conflicts with existing data (e.g. duplicate entry)",
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.warn("Constraint violation on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // --- Catch-all ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                ErrorCode.INTERNAL_ERROR.getCode(),
                "An unexpected error occurred",
                Instant.now(),
                RequestContext.getPath().orElse(null),
                RequestContext.getRequestId().orElse(null)
        );

        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    // --- Helpers ---

    private HttpStatus statusFor(MindBridgeException ex) {
        return switch (ex.getCode()) {
            case RESOURCE_NOT_FOUND, USER_NOT_FOUND,
                 CHAT_SESSION_NOT_FOUND, CHECKIN_ASSIGNMENT_NOT_FOUND,
                 USER_PROGRAM_NOT_FOUND, PROGRAM_NOT_FOUND,
                 BEHAVIOR_PROFILE_NOT_FOUND -> HttpStatus.NOT_FOUND;

            case VALIDATION_ERROR, METHOD_NOT_ALLOWED, MEDIA_TYPE_NOT_ACCEPTABLE -> HttpStatus.BAD_REQUEST;

            case AUTH_CREDENTIALS_INVALID, AUTH_TOKEN_INVALID -> HttpStatus.UNAUTHORIZED;
            case AUTH_TOKEN_EXPIRED -> HttpStatus.UNAUTHORIZED;
            case USER_SUSPENDED, CHAT_SESSION_ACCESS_DENIED, ACCESS_DENIED -> HttpStatus.FORBIDDEN;

            case USER_EMAIL_DUPLICATE, CHECKIN_ANSWER_DUPLICATE,
                 CHAT_SESSION_CLOSED, CONSENT_REVOKED, CONSENT_REQUIRED -> HttpStatus.CONFLICT;

            case MATCHING_SAFETY_BLOCKED, MATCHING_INSUFFICIENT_DATA,
                 MATCHING_NO_CANDIDATE -> HttpStatus.CONFLICT;

            case AI_PROVIDER_TIMEOUT, AI_PROVIDER_UNAVAILABLE,
                 AI_ANALYSIS_OUTPUT_INVALID -> HttpStatus.BAD_GATEWAY;

            case RISK_CLASSIFIER_TIMEOUT, RISK_CLASSIFIER_UNAVAILABLE,
                 RISK_CLASSIFIER_OUTPUT_INVALID -> HttpStatus.BAD_GATEWAY;

            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
