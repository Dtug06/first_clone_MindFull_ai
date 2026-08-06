package com.mindbridge.common.dto;

/**
 * Per-field validation error matching FieldError schema in 03_API_CONTRACT.yaml.
 */
public record FieldError(
        String field,
        String message
) {
}
