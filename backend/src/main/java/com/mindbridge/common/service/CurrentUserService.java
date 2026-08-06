package com.mindbridge.common.service;

import com.mindbridge.auth.filter.JwtAuthenticationFilter.JwtPrincipal;
import com.mindbridge.common.exception.AccessDeniedException;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Provides safe access to the currently authenticated user's identity.
 *
 * All services that need the current userId MUST use this service
 * rather than reading from the request or trusting client input.
 *
 * Security rule: userId comes from the validated JWT — never from request body.
 */
@Service
public class CurrentUserService {

    /**
     * Returns the UUID of the currently authenticated user.
     *
     * @throws AccessDeniedException if no valid authentication is present
     */
    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user");
        }
        return principal.userId();
    }

    /**
     * Returns the role of the currently authenticated user (e.g. "USER", "EXPERT", "ADMIN").
     *
     * @throws AccessDeniedException if no valid authentication is present
     */
    public String getCurrentUserRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user");
        }
        return principal.role();
    }

    /**
     * Returns the JwtPrincipal of the currently authenticated user.
     *
     * @throws AccessDeniedException if no valid authentication is present
     */
    public JwtPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof JwtPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user");
        }
        return principal;
    }

    /**
     * Verifies that the current user owns the given resource userId.
     *
     * @param resourceOwnerId the userId of the resource being accessed
     * @throws AccessDeniedException if the resource does not belong to the current user
     */
    public void verifyOwnership(UUID resourceOwnerId) {
        if (!getCurrentUserId().equals(resourceOwnerId)) {
            throw new AccessDeniedException("You do not have access to this resource");
        }
    }
}
