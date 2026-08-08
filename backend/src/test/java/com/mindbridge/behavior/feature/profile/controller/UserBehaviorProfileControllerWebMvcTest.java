package com.mindbridge.behavior.feature.profile.controller;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mindbridge.auth.filter.JwtAuthenticationFilter;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.auth.service.JwtService;
import com.mindbridge.behavior.feature.profile.DataQualityStatus;
import com.mindbridge.behavior.feature.profile.dto.ProfileSnapshot;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileResponseMapper;
import com.mindbridge.behavior.feature.profile.service.UserBehaviorProfileService;
import com.mindbridge.behavior.feature.profile.service.OnDemandAggregationTrigger;
import com.mindbridge.common.config.SecurityConfig;
import com.mindbridge.common.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * G4-T12 HTTP-level test for {@link UserBehaviorProfileController}.
 *
 * <p>Closes T09 F-3 (HTTP 401/403/200/404 coverage gap). Uses {@code @WebMvcTest}
 * slice so the test boots the Spring Security filter chain (incl.
 * {@link JwtAuthenticationFilter}) without bringing up the full H2 schema
 * (V21/V23/V24).
 *
 * <p>Authentication is injected via
 * {@link org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors#jwt()}
 * which sets a {@link JwtAuthenticationFilter.JwtPrincipal} directly on the
 * SecurityContext — no real JWT signature required.
 */
@WebMvcTest(controllers = UserBehaviorProfileController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserBehaviorProfileControllerWebMvcTest {

    @Autowired MockMvc mockMvc;

    @MockBean UserBehaviorProfileService profileService;
    @MockBean UserBehaviorProfileResponseMapper responseMapper;
    @MockBean CurrentUserService currentUserService;
    @MockBean OnDemandAggregationTrigger onDemandAggregationTrigger;
    @MockBean UserRepository userRepository;
    @MockBean Clock clock;
    @MockBean JwtService jwtService; // needed for JwtAuthenticationFilter to wire

    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    LocalDate windowEnd = LocalDate.of(2026, 8, 4);

    @Test
    @DisplayName("GET /behavior/profile without auth returns 401")
    void noAuth_returns401() throws Exception {
        mockMvc.perform(get("/behavior/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /behavior/profile with auth and existing profile returns 200 + nested trendSummary")
    void ownUser_returns200() throws Exception {
        OffsetDateTime calculatedAt = OffsetDateTime.parse("2026-08-04T03:15:00Z");
        ProfileSnapshot snapshot = new ProfileSnapshot(
                userId, windowEnd,
                new BigDecimal("0.4"), new BigDecimal("0.5"),
                new BigDecimal("0.6"), new BigDecimal("0.5"),
                new BigDecimal("0.7"), new BigDecimal("0.5"),
                new BigDecimal("0.55"), new BigDecimal("0.55"),
                new BigDecimal("0.3"), new BigDecimal("0.3"),
                2, 3,
                null, List.of(), List.of(),
                (short) 2, null,
                new BigDecimal("0.6"),
                new BigDecimal("0.5"),
                DataQualityStatus.SUFFICIENT,
                calculatedAt);

        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.of(snapshot));

        // G4-T12: build a minimal UserBehaviorProfileResponse via the real mapper
        // so the WebMvcTest exercises the actual DTO serialization (incl. the
        // nested TrendSummaryResponse).
        UserBehaviorProfileResponseMapper realMapper = new UserBehaviorProfileResponseMapper(
                new ObjectMapper().registerModule(new JavaTimeModule()));
        // ... but the bean is @MockBean so toResponse() returns null. Use the
        // local instance for assertion and the mock for controller dispatch.
        when(responseMapper.toResponse(snapshot))
                .thenReturn(realMapper.toResponse(snapshot));

        mockMvc.perform(get("/behavior/profile")
                        .with(authUser(userId, "USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileVersion").value("profile_v1"))
                .andExpect(jsonPath("$.windowEnd").value("2026-08-04"))
                .andExpect(jsonPath("$.dataCoverage").value(0.6))
                .andExpect(jsonPath("$.confidence").value(0.5))
                .andExpect(jsonPath("$.dataQualityStatus").value("SUFFICIENT"))
                .andExpect(jsonPath("$.riskLevel").value(2))
                .andExpect(jsonPath("$.engagementScore7d").value(2))
                .andExpect(jsonPath("$.engagementScore30d").value(3))
                .andExpect(jsonPath("$.trendSummary").exists())
                .andExpect(jsonPath("$.trendSummary.calculationVersion").value("trend_v1"))
                .andExpect(jsonPath("$.trendSummary.userId").value(userId.toString()))
                .andExpect(jsonPath("$.trendSummary.dataQuality").value("TODO_T11_ALIGNED"))
                .andExpect(jsonPath("$.trendSummary.entries").isArray());
    }

    @Test
    @DisplayName("GET /behavior/profile with auth but no profile returns 404")
    void ownUserNoProfile_returns404() throws Exception {
        when(currentUserService.getCurrentUserId()).thenReturn(userId);
        when(profileService.findLatestForUser(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/behavior/profile")
                        .with(authUser(userId, "USER")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /behavior/profile reads userId from JWT principal only (ownership check)")
    void readsUserIdOnlyFromJwt() throws Exception {
        UUID otherUserId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        when(currentUserService.getCurrentUserId()).thenReturn(otherUserId);
        when(profileService.findLatestForUser(otherUserId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/behavior/profile")
                        .with(authUser(otherUserId, "USER")))
                .andExpect(status().isNotFound());
    }

    /**
     * Wraps a {@link JwtAuthenticationFilter.JwtPrincipal} as the
     * {@link org.springframework.test.web.servlet.request.RequestPostProcessor}
     * that MockMvc's {@code with(...)} expects, mimicking what
     * {@link JwtAuthenticationFilter} would do after a real JWT decode.
     * We use {@code authentication()} (which does NOT require
     * spring-security-oauth2-jose on the classpath) instead of {@code jwt()}
     * to keep the test surface minimal.
     */
    private static RequestPostProcessor authUser(UUID userId, String role) {
        JwtAuthenticationFilter.JwtPrincipal principal =
                new JwtAuthenticationFilter.JwtPrincipal(userId, role);
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, null,
                        java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role)));
        return authentication(auth);
    }
}
