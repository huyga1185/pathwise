package uk.huy.pathwise.auth.infrastructure.security;

import uk.huy.pathwise.shared.identity.AuthenticatedUser;

/**
 * Authorization rules that a role alone cannot express, for {@code @PreAuthorize} (ARCHITECTURE.md §6.3). Registered
 * as bean {@code authz} by {@code SecurityConfig}, so web slice tests that import {@code SecurityConfig} get it too.
 * <p>
 * Example for the driver API (T24, T30): {@code @PreAuthorize("@authz.canDrive(principal)")}.
 */
public class AuthorizationRules {
    /**
     * @return true if the caller can be assigned routes: {@code role = DRIVER} or {@code canDrive} (D-21).
     * Whether a given route is the caller's is checked in the service ({@code route.driver_id = actor.id()}).
     */
    public boolean canDrive(Object principal) {
        return principal instanceof AuthenticatedUser user && user.identity().assignableToRoutes();
    }
}
