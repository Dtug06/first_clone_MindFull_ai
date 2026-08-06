package com.mindbridge.common.dto;

import java.time.Instant;

/**
 * Response body for {@code GET /api/v1/health}.
 * Matches the {@code HealthResponse} schema in 03_API_CONTRACT.yaml.
 */
public record HealthResponse(String status, Instant timestamp) {
}