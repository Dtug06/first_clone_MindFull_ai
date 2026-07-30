package com.mindbridge.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindbridge.auth.filter.JwtAuthenticationFilter.JwtPrincipal;
import com.mindbridge.common.exception.AccessDeniedException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for CurrentUserService.
 *
 * Pure unit tests — no Spring context needed.
 * CurrentUserService uses only static SecurityContextHolder.
 */
@DisplayName("CurrentUserService")
class CurrentUserServiceTest {

    private final CurrentUserService service = new CurrentUserService();

    private static final UUID ALICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID BOB_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private void setAuth(UUID userId, String role) {
        var principal = new JwtPrincipal(userId, role);
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetCurrentUserId {

        @Test
        void authenticated() {
            setAuth(ALICE_ID, "USER");

            UUID id = service.getCurrentUserId();

            assertThat(id).isEqualTo(ALICE_ID);
        }

        @Test
        void noAuth() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> service.getCurrentUserId())
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("No authenticated user");
        }

        @Test
        void wrongPrincipalType() {
            var auth = new UsernamePasswordAuthenticationToken("anonymous", null);
            SecurityContextHolder.getContext().setAuthentication(auth);

            assertThatThrownBy(() -> service.getCurrentUserId())
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class GetCurrentUserRole {

        @Test
        void returnsRole() {
            setAuth(ALICE_ID, "EXPERT");

            String role = service.getCurrentUserRole();

            assertThat(role).isEqualTo("EXPERT");
        }
    }

    @Nested
    class VerifyOwnership {

        @Test
        void owner_passes() {
            setAuth(ALICE_ID, "USER");

            service.verifyOwnership(ALICE_ID); // must not throw
        }

        @Test
        void notOwner_throws() {
            setAuth(ALICE_ID, "USER");

            assertThatThrownBy(() -> service.verifyOwnership(BOB_ID))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("You do not have access to this resource");
        }

        @Test
        void noAuth_throws() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> service.verifyOwnership(ALICE_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    class GetCurrentPrincipal {

        @Test
        void returnsFullPrincipal() {
            setAuth(ALICE_ID, "ADMIN");

            JwtPrincipal principal = service.getCurrentPrincipal();

            assertThat(principal.userId()).isEqualTo(ALICE_ID);
            assertThat(principal.role()).isEqualTo("ADMIN");
        }
    }
}
