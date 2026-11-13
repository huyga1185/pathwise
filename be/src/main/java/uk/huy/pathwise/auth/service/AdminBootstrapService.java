package uk.huy.pathwise.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.infrastructure.config.BootstrapAdminProperties;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserAccountService;

/**
 * Creates the first platform ADMIN at startup from {@code bootstrap.admin.*} (T59, P-59), so a new deployment needs no
 * SQL. Runs only while no ADMIN exists; never logs the password. A wrong setting is logged and skipped: it does not
 * stop the application.
 * <p>
 * Without a password, the ADMIN gets a random one nobody knows, and a reset OTP is emailed right away; the ADMIN then
 * sets a password with "Quên mật khẩu" (the OTP of that email, or a new one asked there).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(BootstrapAdminProperties.class)
public class AdminBootstrapService {
    private static final int MIN_PASSWORD = 8;
    private static final int MAX_PASSWORD = 72;

    private final BootstrapAdminProperties properties;
    private final UserAccountService userAccountService;
    private final OpaqueTokenGenerator tokenGenerator;
    private final OtpManager otpManager;

    public enum Outcome {
        /** No {@code bootstrap.admin.email}. */
        NOT_CONFIGURED,
        /** An ADMIN already exists: nothing created. */
        ADMIN_EXISTS,
        /** The password does not satisfy the length rule (8 to 72). */
        INVALID_PASSWORD,
        /** The email belongs to another account (an OWNER, a driver…). */
        EMAIL_TAKEN,
        CREATED
    }

    @EventListener(ApplicationReadyEvent.class)
    void onReady() {
        bootstrap();
    }

    /**
     * Creates the ADMIN if configured and none exists. Called at startup; public for tests.
     */
    public Outcome bootstrap() {
        if (!properties.configured()) return Outcome.NOT_CONFIGURED;
        if (userAccountService.adminExists()) {
            log.warn("bootstrap.admin is set but an ADMIN already exists: nothing created. Remove BOOTSTRAP_ADMIN_* "
                    + "from the environment.");
            return Outcome.ADMIN_EXISTS;
        }
        String password = properties.hasPassword() ? properties.password() : tokenGenerator.generateOpaqueToken();
        if (password.length() < MIN_PASSWORD || password.length() > MAX_PASSWORD) {
            log.error("bootstrap.admin.password must have {} to {} characters: no ADMIN created", MIN_PASSWORD, MAX_PASSWORD);
            return Outcome.INVALID_PASSWORD;
        }
        UserIdentity admin;
        try {
            admin = userAccountService.createAdmin(properties.email(), password, properties.fullName());
        } catch (AppException e) {
            if (e.getErrorCode() != ErrorCode.EMAIL_EXISTED) throw e;
            log.error("bootstrap.admin.email belongs to an existing account: no ADMIN created");
            return Outcome.EMAIL_TAKEN;
        }
        log.info("First ADMIN created. userId={}", admin.id());
        if (!properties.hasPassword()) sendResetOtp(admin);
        return Outcome.CREATED;
    }

    /**
     * After the commit of the ADMIN, outside any transaction. A failure only means the ADMIN asks for a new OTP.
     */
    private void sendResetOtp(UserIdentity admin) {
        try {
            otpManager.sendOtp(OtpPurpose.RESET_PASSWORD, admin.id(), admin.email());
            log.info("Password reset OTP emailed to the first ADMIN. userId={}", admin.id());
        } catch (RuntimeException e) {
            log.warn("Password reset OTP not sent to the first ADMIN; use \"Quên mật khẩu\". userId={}", admin.id());
        }
    }
}
