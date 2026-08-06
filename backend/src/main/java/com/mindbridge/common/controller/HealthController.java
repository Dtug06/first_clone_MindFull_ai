package com.mindbridge.common.controller;

import com.mindbridge.common.dto.HealthResponse;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Custom health endpoint exposed at {@code GET /api/v1/health}.
 *
 * Distinct from the Spring Boot Actuator endpoint ({@code /actuator/health})
 * so the contract document and the underlying ops tool stay independent.
 * The body format matches the {@code HealthResponse} schema in
 * {@code docs/03_API_CONTRACT.yaml}.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public HealthResponse getHealth() {
        return new HealthResponse("UP", Instant.now());
    }
}