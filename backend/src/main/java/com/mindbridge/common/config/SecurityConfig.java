package com.mindbridge.common.config;

import com.mindbridge.auth.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Application-level Spring Security configuration.
 *
 * - Stateless JWT authentication — no server-side sessions.
 * - Auth endpoints (register, login) are publicly accessible.
 * - All other endpoints require a valid JWT.
 * - Custom exception handling: 401 and 403 return JSON (not redirects).
 * - Method security enabled for @PreAuthorize on endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // G1-T10: public health endpoint + Swagger UI / OpenAPI docs
                        .requestMatchers("/health", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> {
                    // 401 — unauthenticated (no valid JWT)
                    ex.authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"code\":\"AUTH_TOKEN_INVALID\",\"message\":\"Authentication required\",\"timestamp\":\"" +
                                java.time.Instant.now().toString() + "\"}"
                        );
                    });
                    // 403 — authenticated but not authorized (wrong role or ownership failure)
                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"code\":\"ACCESS_DENIED\",\"message\":\"You do not have permission to access this resource\",\"timestamp\":\"" +
                                java.time.Instant.now().toString() + "\"}"
                        );
                    });
                })
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default BCrypt strength: 10 rounds — industry standard for MVP
        return new BCryptPasswordEncoder();
    }
}
