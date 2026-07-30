package com.mindbridge.common.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit-level tests for {@link LoggingRequestContextFilter}.
 * Verifies requestId generation, MDC propagation, and response header echo.
 */
@DisplayName("LoggingRequestContextFilter")
class LoggingRequestContextFilterTest {

    private final LoggingRequestContextFilter filter = new LoggingRequestContextFilter();

    @AfterEach
    void cleanup() {
        MDC.clear();
    }

    @Test
    @DisplayName("Generates a requestId when X-Request-Id is absent and writes it to the response")
    void generatedRequestId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        MockHttpServletResponse res = new MockHttpServletResponse();

        try {
            filter.doFilter(req, res, (request, response) -> {
                assertThat(MDC.get("requestId")).isNotBlank();
                assertThat(MDC.get("path")).isEqualTo("/api/x");
            });
        } catch (Exception ignored) {
        }

        assertThat(res.getHeader("X-Request-Id")).isNotBlank();
    }

    @Test
    @DisplayName("Echoes the inbound X-Request-Id header when present")
    void echoesInboundHeader() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader("X-Request-Id", "inbound-xyz");
        MockHttpServletResponse res = new MockHttpServletResponse();

        try {
            filter.doFilter(req, res, (request, response) -> {
                assertThat(MDC.get("requestId")).isEqualTo("inbound-xyz");
            });
        } catch (Exception ignored) {
        }

        assertThat(res.getHeader("X-Request-Id")).isEqualTo("inbound-xyz");
    }

    @Test
    @DisplayName("Clears MDC keys in finally even when downstream throws")
    void clearsMdcOnError() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        MockHttpServletResponse res = new MockHttpServletResponse();

        try {
            filter.doFilter(req, res, (request, response) -> {
                throw new RuntimeException("boom");
            });
        } catch (RuntimeException expected) {
            assertThat(expected).hasMessage("boom");
        }

        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("path")).isNull();
    }
}