package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserIdentity;

/**
 * Outcome of checking log-in credentials (P-24). The password is checked first, so the account status is only
 * revealed to someone who knows it.
 */
public sealed interface AuthenticationResult {
    record Authenticated(UserIdentity identity, String fullName, boolean canDrive) implements AuthenticationResult {}

    /** Unknown email or wrong password; the two are not told apart. */
    record InvalidCredentials() implements AuthenticationResult {}

    record EmailNotVerified() implements AuthenticationResult {}

    record Disabled() implements AuthenticationResult {}
}
