package uk.huy.pathwise.auth.infrastructure.security;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationRulesTest {
    private final AuthorizationRules rules = new AuthorizationRules();

    @Test
    void canDrive_whenDriverOrFlaggedOwner_returnTrue() {
        assertThat(rules.canDrive(principal(UserRole.DRIVER, false))).isTrue();
        assertThat(rules.canDrive(principal(UserRole.OWNER, true))).isTrue();
    }

    @Test
    void canDrive_whenDispatcherWithoutFlag_returnFalse() {
        assertThat(rules.canDrive(principal(UserRole.DISPATCHER, false))).isFalse();
    }

    @Test
    void canDrive_whenAnonymousPrincipal_returnFalse() {
        assertThat(rules.canDrive("anonymousUser")).isFalse();
        assertThat(rules.canDrive(null)).isFalse();
    }

    private static AuthenticatedUser principal(UserRole role, boolean canDrive) {
        return new AuthenticatedUser(new UserIdentity(1L, "u@pathwise.test", role, 1L, canDrive), "s");
    }
}
