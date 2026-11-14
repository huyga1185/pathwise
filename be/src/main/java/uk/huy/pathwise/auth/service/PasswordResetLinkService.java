package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.auth.dto.request.ResetLinkOtpRequest;
import uk.huy.pathwise.auth.dto.request.ResetPasswordByLinkRequest;
import uk.huy.pathwise.auth.infrastructure.ratelimit.AuthRateLimitPolicy;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.resetlink.ResetLinkStore;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.AccountState;
import uk.huy.pathwise.user.service.UserAccountService;

/**
 * The page a password reset link of an ADMIN opens (T57, P-63). The link alone is not enough: the password changes
 * only with a code sent to the account's email at that moment. The page never learns the email.
 */
@Slf4j
@Service
public class PasswordResetLinkService {
    private final ResetLinkStore resetLinkStore;
    private final Hasher hasher;
    private final UserAccountService userService;
    private final OtpManager otpManager;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthRateLimitPolicy rateLimitPolicy;
    private final TransactionTemplate transactionTemplate;

    public PasswordResetLinkService(ResetLinkStore resetLinkStore,
                                    Hasher hasher,
                                    UserAccountService userService,
                                    OtpManager otpManager,
                                    RefreshTokenManager refreshTokenManager,
                                    AuthRateLimitPolicy rateLimitPolicy,
                                    PlatformTransactionManager transactionManager) {
        this.resetLinkStore = resetLinkStore;
        this.hasher = hasher;
        this.userService = userService;
        this.otpManager = otpManager;
        this.refreshTokenManager = refreshTokenManager;
        this.rateLimitPolicy = rateLimitPolicy;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Emails a {@code RESET_PASSWORD} code to the account of the link. The link stays valid.
     *
     * @throws AppException {@code INVALID_RESET_TOKEN} for an unknown, expired or used link, or an account no longer
     *                      ACTIVE; {@code RATE_LIMITED}; {@code OTP_COOLDOWN} within 60 s of the previous code
     */
    public void sendOtp(ClientDetail clientDetail, ResetLinkOtpRequest request) {
        AccountState account = accountOf(request.token());
        AccountSupportService.enforce(() -> rateLimitPolicy.forgotPassword(clientDetail.ip(), account.email()));
        if (otpManager.sendOtp(OtpPurpose.RESET_PASSWORD, account.userId(), account.email()) == OtpSendResult.COOLDOWN)
            throw new AppException(ErrorCode.OTP_COOLDOWN);
    }

    /**
     * Sets the new password and revokes every session, in one transaction; the link is then used up.
     *
     * @throws AppException {@code INVALID_RESET_TOKEN} as above; {@code INVALID_OTP} for a wrong or expired code
     */
    public void resetPassword(ResetPasswordByLinkRequest request) {
        String tokenHash = hasher.hashString(request.token().trim());
        AccountState account = accountOf(request.token());
        if (!otpManager.verifyOtp(OtpPurpose.RESET_PASSWORD, account.userId(), request.otp()))
            throw new AppException(ErrorCode.INVALID_OTP);
        transactionTemplate.executeWithoutResult(status -> {
            userService.updateUserPassword(account.userId(), request.newPassword());
            refreshTokenManager.revokeAllForUser(account.userId(), null);
        });
        resetLinkStore.delete(account.userId(), tokenHash);
        log.info("Password reset with an ADMIN's link, all sessions revoked. userId={}", account.userId());
    }

    private AccountState accountOf(String token) {
        long userId = resetLinkStore.findUserId(hasher.hashString(token.trim()))
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));
        return userService.findUserById(userId)
                .flatMap(user -> userService.findAccountState(user.email()))
                .filter(account -> account.status() == UserStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));
    }
}
