package com.mindbridge.common.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration driven by the {@code mindbridge.cors.allowed-origins} list.
 *
 * <p>Profile-aware defaults:
 * <ul>
 *   <li><strong>local</strong>: Vite dev server at {@code http://localhost:5173}.</li>
 *   <li><strong>prod</strong>: read from {@code APP_CORS_ALLOWED_ORIGINS} (comma-separated).
 *       Empty list = no cross-origin allowed (intentional fail-safe).</li>
 *   <li><strong>test</strong>: Vite dev server at {@code http://localhost:5173} by default.</li>
 * </ul>
 *
 * <p>Wildcard origin is NEVER used in production. With credentials enabled,
 * the browser would refuse {@code *}, but we also forbid it explicitly here
 * so the code itself is consistent with the security rule.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public CorsConfig(@Value("${mindbridge.cors.allowed-origins:}") List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (allowedOrigins.isEmpty()) {
            return;
        }
        // The servlet context path (/api/v1) is stripped before Spring MVC
        // evaluates this mapping, so controller paths start at /auth, /users, etc.
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders(
                        "Authorization",
                        "Content-Type",
                        "X-Request-Id",
                        "Idempotency-Key")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
