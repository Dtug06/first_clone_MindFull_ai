package com.mindbridge.common.util;

import java.util.Optional;
import org.slf4j.MDC;

/**
 * Holds request-scoped context (e.g. requestId from MDC) that needs to be
 * propagated into response DTOs outside the HTTP thread boundary.
 */
public final class RequestContext {

    private RequestContext() {
    }

    /**
     * Returns the current requestId stored in MDC, if any.
     */
    public static Optional<String> getRequestId() {
        return Optional.ofNullable(MDC.get("requestId"));
    }

    /**
     * Returns the current request URI stored in MDC, if any.
     */
    public static Optional<String> getPath() {
        return Optional.ofNullable(MDC.get("path"));
    }
}
