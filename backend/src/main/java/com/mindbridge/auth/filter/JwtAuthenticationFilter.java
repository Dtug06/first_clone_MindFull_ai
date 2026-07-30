package com.mindbridge.auth.filter;

import com.mindbridge.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Extracts the Bearer token from the Authorization header, validates it,
 * and populates the Spring Security context.
 *
 * If no token is present, the filter passes through — the SecurityFilterChain
 * will return 401 for protected endpoints.
 *
 * Security rules enforced here:
 * - Token values are never logged.
 * - Only valid tokens set the authentication context.
 * - Silent failure for malformed tokens (not logged, not exposed).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            if (jwtService.isTokenValid(token)) {
                try {
                    UUID userId = jwtService.extractUserId(token);
                    String role = jwtService.extractRole(token);

                    var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role));

                    var principal = new JwtPrincipal(userId, role);

                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } catch (Exception e) {
                    // Token was valid but claims could not be parsed — do not authenticate
                    // Do not log the token or the exception details
                    SecurityContextHolder.clearContext();
                }
            }
            // If token is invalid → SecurityContext stays empty → chain proceeds,
            // Spring Security will reject at the end.
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Holds the authenticated principal for downstream use.
     */
    public record JwtPrincipal(UUID userId, String role) {
    }
}
