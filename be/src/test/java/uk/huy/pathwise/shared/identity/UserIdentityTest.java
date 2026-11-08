package uk.huy.pathwise.shared.identity;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class UserIdentityTest {
    @ParameterizedTest(name = "{0} canDrive={1} -> {2}")
    @CsvSource({
            "DRIVER, false, true",
            "DRIVER, true, true",
            "OWNER, false, false",
            "OWNER, true, true",
            "DISPATCHER, false, false",
            "DISPATCHER, true, true",
            "ADMIN, true, false"
    })
    void assignableToRoutes_whenRoleAndFlag_followD21(UserRole role, boolean canDrive, boolean expected) {
        assertThat(new UserIdentity(1L, "u@pathwise.test", role, role == UserRole.ADMIN ? null : 1L, canDrive)
                .assignableToRoutes()).isEqualTo(expected);
    }
}
