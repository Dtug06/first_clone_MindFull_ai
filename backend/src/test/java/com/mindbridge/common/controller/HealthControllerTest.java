package com.mindbridge.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration test for the public health endpoint and the OpenAPI surface.
 *
 * Verifies:
 * - GET /api/v1/health returns the HealthResponse contract shape.
 * - GET /api/v1/v3/api-docs returns the OpenAPI JSON and lists the
 *   bearerAuth security scheme (so Swagger UI can lock requests).
 * - GET /api/v1/swagger-ui.html renders the Swagger UI HTML.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Health and OpenAPI integration")
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health returns 200 with status UP and timestamp")
    void healthReturnsUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("GET /api/v1/v3/api-docs returns OpenAPI JSON with bearerAuth scheme")
    void openApiJsonIncludesBearer() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type")
                        .value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme")
                        .value("bearer"));
    }

    @Test
    @DisplayName("Swagger UI is reachable (redirect or HTML)")
    void swaggerUiRenders() throws Exception {
        // springdoc redirects /swagger-ui.html to /swagger-ui/index.html.
        // Accept either 200 HTML or 302 redirect to the new path.
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .status().is(org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.equalTo(200),
                                org.hamcrest.Matchers.equalTo(302))));
    }
}