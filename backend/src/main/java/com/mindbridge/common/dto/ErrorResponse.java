package com.mindbridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error response matching ErrorResponse schema in 03_API_CONTRACT.yaml.
 * Contains code, message, timestamp, optional path, optional requestId,
 * and optional fieldErrors list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        String requestId,
        List<FieldError> fieldErrors
) {

    public ErrorResponse(String code, String message, Instant timestamp) {
        this(code, message, timestamp, null, null, null);
    }

    public ErrorResponse(String code, String message, Instant timestamp, String path, String requestId) {
        this(code, message, timestamp, path, requestId, null);
    }
}
