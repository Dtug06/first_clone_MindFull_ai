package com.mindbridge.common.dto;

import java.util.List;

/**
 * Standard paginated response wrapper matching ChatSessionPageResponse /
 * ChatMessagePageResponse schema in 03_API_CONTRACT.yaml.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
