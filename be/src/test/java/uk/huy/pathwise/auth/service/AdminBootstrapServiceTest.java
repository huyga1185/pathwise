package uk.huy.pathwise.auth.service;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.auth.infrastructure.config.BootstrapAdminProperties;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.service.AdminBootstrapService.Outcome;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.service.UserAccountService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminBootstrapServiceTest {
    private static final String RANDOM = "r4nd0m-0paque-t0ken-of-43-characters-xxxxx";
    private final UserAccountService users = mock(UserAccountService.class);
    private final OpaqueTokenGenerator tokens = mock(OpaqueTokenGenerator.class);
    private final OtpManager otpManager = mock(OtpManager.class);

    private AdminBootstrapService service(String email, String password) {
        when(tokens.generateOpaqueToken()).thenReturn(RANDOM);
        return new AdminBootstrapService(new BootstrapAdminProperties(email, password, "Admin"), users, tokens, otpManager);
    }

    private static UserIdentity admin() {
        return new UserIdentity(7, "admin@pathwise.test", UserRole.ADMIN, null, false);
    }

    @Test
    void bootstrap_whenNoEmail_doNothing() {
        assertThat(service(null, null).bootstrap()).isEqualTo(Outcome.NOT_CONFIGURED);
        assertThat(service("  ", "matkhau-123").bootstrap()).isEqualTo(Outcome.NOT_CONFIGURED);

        verify(users, never()).adminExists();
        verify(users, never()).createAdmin(anyString(), anyString(), anyString());
    }

    @Test
    void bootstrap_whenRunTwice_createOneAdminThenSkip() {
        when(users.adminExists()).thenReturn(false, true);
        when(users.createAdmin("Admin@PathWise.test", "matkhau-123", "Admin")).thenReturn(admin());
        AdminBootstrapService service = service("Admin@PathWise.test", "matkhau-123");

        assertThat(service.bootstrap()).isEqualTo(Outcome.CREATED);
        assertThat(service.bootstrap()).isEqualTo(Outcome.ADMIN_EXISTS);

        verify(users, times(1)).createAdmin(anyString(), anyString(), anyString());
        // A password was given: no reset email.
        verify(otpManager, never()).sendOtp(any(), anyLong(), anyString());
    }

    @Test
    void bootstrap_whenNoPassword_useRandomPasswordAndEmailResetOtp() {
        when(users.createAdmin("admin@pathwise.test", RANDOM, "Admin")).thenReturn(admin());

        assertThat(service("admin@pathwise.test", "").bootstrap()).isEqualTo(Outcome.CREATED);

        verify(users).createAdmin("admin@pathwise.test", RANDOM, "Admin");
        verify(otpManager).sendOtp(OtpPurpose.RESET_PASSWORD, 7, "admin@pathwise.test");
    }

    @Test
    void bootstrap_whenResetEmailFails_stillCreated() {
        when(users.createAdmin(anyString(), anyString(), anyString())).thenReturn(admin());
        when(otpManager.sendOtp(any(), anyLong(), anyString())).thenThrow(new IllegalStateException("SMTP down"));

        assertThat(service("admin@pathwise.test", null).bootstrap()).isEqualTo(Outcome.CREATED);
    }

    @Test
    void bootstrap_whenPasswordTooShortOrTooLong_createNothing() {
        assertThat(service("admin@pathwise.test", "short").bootstrap()).isEqualTo(Outcome.INVALID_PASSWORD);
        assertThat(service("admin@pathwise.test", "x".repeat(73)).bootstrap()).isEqualTo(Outcome.INVALID_PASSWORD);

        verify(users, never()).createAdmin(anyString(), anyString(), anyString());
    }

    @Test
    void bootstrap_whenEmailBelongsToAnotherAccount_createNothing() {
        when(users.createAdmin(anyString(), anyString(), anyString())).thenThrow(new AppException(ErrorCode.EMAIL_EXISTED));

        assertThat(service("owner@pathwise.test", "matkhau-123").bootstrap()).isEqualTo(Outcome.EMAIL_TAKEN);
    }
}
