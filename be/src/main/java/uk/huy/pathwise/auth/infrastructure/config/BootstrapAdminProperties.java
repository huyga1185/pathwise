package uk.huy.pathwise.auth.infrastructure.config;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code bootstrap.admin.*} (T59): the first platform ADMIN, created at startup when there is none. Usually from the
 * environment: {@code BOOTSTRAP_ADMIN_EMAIL}, {@code BOOTSTRAP_ADMIN_PASSWORD}, {@code BOOTSTRAP_ADMIN_FULL_NAME}.
 *
 * @param email    the ADMIN's email; empty: no bootstrap
 * @param password its password (8 to 72 characters); empty: a random one, and a reset OTP is emailed
 * @param fullName its display name
 */
@ConfigurationProperties("bootstrap.admin")
public record BootstrapAdminProperties(@Nullable String email,
                                       @Nullable String password,
                                       @DefaultValue("Admin") String fullName) {
    public boolean configured() {
        return email != null && !email.isBlank();
    }

    public boolean hasPassword() {
        return password != null && !password.isEmpty();
    }
}
