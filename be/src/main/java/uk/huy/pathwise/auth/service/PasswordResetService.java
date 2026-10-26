package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.auth.dto.request.ForgotPasswordRequest;
import uk.huy.pathwise.auth.dto.request.ResetPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyResetOtpRequest;
import uk.huy.pathwise.auth.dto.response.ResetTokenResponse;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpToken;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpTokenVerification;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.AccountState;
import uk.huy.pathwise.user.service.UserAccountService;

import java.time.Duration;

/**
 * Forgotten password in three steps (P-06, FR-A07): {@code forgot} emails an OTP, {@code reset/verify} trades the
 * OTP for a single-use reset token bound to the user, {@code reset} sets the new password and revokes every session.
 * Nothing in the responses tells whether an email is registered.
 */
@Service
@Slf4j
public class PasswordResetService {
    private static final String RESET_TOKEN_PURPOSE = OtpPurpose.RESET_PASSWORD.name();
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    // The reset token given to the client is "{ticket}.{secret}"; both parts are URL-safe Base64, without dots.
    private static final char RESET_TOKEN_SEPARATOR = '.';

    private final UserAccountService userService;
    private final OtpManager otpManager;
    private final OtpTokenManager otpTokenManager;
    private final RefreshTokenManager refreshTokenManager;
    private final TransactionTemplate transactionTemplate;

    public PasswordResetService(UserAccountService userService,
                                OtpManager otpManager,
                                OtpTokenManager otpTokenManager,
                                RefreshTokenManager refreshTokenManager,
                                PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.otpManager = otpManager;
        this.otpTokenManager = otpTokenManager;
        this.refreshTokenManager = refreshTokenManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Emails a {@code RESET_PASSWORD} OTP if the email belongs to an ACTIVE account. Always succeeds: an unknown or
     * inactive email, a running cooldown or a failed email are only logged.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        if (request == null) throw new NullPointerException("Forgot password request could not be null");
        userService.findAccountState(request.email())
                .filter(account -> account.status() == UserStatus.ACTIVE)
                .ifPresent(this::sendResetOtp);
    }

    /**
     * @throws AppException {@code INVALID_OTP} for a wrong or expired OTP, or an email without an ACTIVE account
     */
    public ResetTokenResponse verifyResetOtp(VerifyResetOtpRequest request) {
        if (request == null) throw new NullPointerException("Verify reset OTP request could not be null");
        AccountState account = userService.findAccountState(request.email())
                .filter(found -> found.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));
        if (!otpManager.verifyOtp(OtpPurpose.RESET_PASSWORD, account.userId(), request.otp()))
            throw new AppException(ErrorCode.INVALID_OTP);
        OtpToken token = otpTokenManager.generateToken(RESET_TOKEN_PURPOSE, account.userId(), RESET_TOKEN_TTL);
        return new ResetTokenResponse(token.ticket() + RESET_TOKEN_SEPARATOR + token.token(), token.expiresAfter());
    }

    /**
     * Sets the new password and revokes every session of the user, in one transaction. The reset token is consumed
     * by the first attempt, even a failed one.
     *
     * @throws AppException {@code INVALID_RESET_TOKEN} if the token is malformed, unknown, expired or already used
     */
    public void resetPassword(ResetPasswordRequest request) {
        if (request == null) throw new NullPointerException("Reset password request could not be null");
        long userId = verifyResetToken(request.resetToken());
        transactionTemplate.executeWithoutResult(status -> {
            userService.updateUserPassword(userId, request.newPassword());
            refreshTokenManager.revokeAllForUser(userId, null);
        });
        log.info("Password reset, all sessions revoked. userId={}", userId);
    }

    private long verifyResetToken(String resetToken) {
        int separator = resetToken.indexOf(RESET_TOKEN_SEPARATOR);
        if (separator <= 0 || separator == resetToken.length() - 1)
            throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
        return switch (otpTokenManager.verifyOtpToken(resetToken.substring(0, separator),
                resetToken.substring(separator + 1),
                RESET_TOKEN_PURPOSE)) {
            case OtpTokenVerification.Verified verified -> verified.userId();
            case OtpTokenVerification.Rejected rejected -> {
                log.info("Reset token rejected: {}", rejected.reason());
                throw new AppException(ErrorCode.INVALID_RESET_TOKEN);
            }
        };
    }

    private void sendResetOtp(AccountState account) {
        try {
            if (otpManager.sendOtp(OtpPurpose.RESET_PASSWORD, account.userId(), account.email()) == OtpSendResult.COOLDOWN)
                log.info("Reset OTP not sent, cooldown running. userId={}", account.userId());
        } catch (EmailDeliveryException e) {
            // OtpManager already released the cooldown, so the user can ask again right away.
            log.warn("Reset OTP email not sent. userId={}", account.userId());
        }
    }
}
