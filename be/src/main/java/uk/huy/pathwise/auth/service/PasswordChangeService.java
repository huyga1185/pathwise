package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.auth.dto.request.ChangePasswordRequest;
import uk.huy.pathwise.auth.dto.request.RequestPasswordChangeOtpRequest;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.user.service.UserAccountService;

/**
 * Password change while logged in (D-17, P-05, FR-A08): the current password unlocks an emailed OTP, so a leaked
 * access token alone can neither change the password nor make the server send emails. The OTP is bound to the
 * caller's user id.
 */
@Service
@Slf4j
public class PasswordChangeService {
    private final UserAccountService userService;
    private final OtpManager otpManager;
    private final RefreshTokenManager refreshTokenManager;
    private final TransactionTemplate transactionTemplate;

    public PasswordChangeService(UserAccountService userService,
                                 OtpManager otpManager,
                                 RefreshTokenManager refreshTokenManager,
                                 PlatformTransactionManager transactionManager) {
        this.userService = userService;
        this.otpManager = otpManager;
        this.refreshTokenManager = refreshTokenManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * @throws AppException {@code INVALID_CURRENT_PASSWORD} (no email is sent); {@code OTP_COOLDOWN} if a code was
     *                      sent less than {@code otp.cooldown} ago
     * @throws uk.huy.pathwise.notification.service.EmailDeliveryException if the email could not be sent (500); the
     *                      cooldown is released, so the user can retry at once
     */
    public void requestChangeOtp(AuthenticatedUser actor, RequestPasswordChangeOtpRequest request) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (request == null) throw new NullPointerException("Request password change OTP request could not be null");
        long userId = actor.identity().id();
        if (!userService.matchesPassword(userId, request.currentPassword()))
            throw new AppException(ErrorCode.INVALID_CURRENT_PASSWORD);
        if (otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, userId, actor.identity().email()) == OtpSendResult.COOLDOWN)
            throw new AppException(ErrorCode.OTP_COOLDOWN);
    }

    /**
     * Sets the new password and revokes every session except the caller's, in one transaction. An access token
     * without {@code sid} (issued before T11) has no current session, so every session is revoked.
     *
     * @throws AppException {@code INVALID_OTP} if {@code otp} is not the caller's current {@code CHANGE_PASSWORD} OTP
     */
    public void changePassword(AuthenticatedUser actor, ChangePasswordRequest request) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (request == null) throw new NullPointerException("Change password request could not be null");
        long userId = actor.identity().id();
        if (!otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, request.otp()))
            throw new AppException(ErrorCode.INVALID_OTP);
        transactionTemplate.executeWithoutResult(status -> {
            userService.updateUserPassword(userId, request.newPassword());
            refreshTokenManager.revokeAllForUser(userId, actor.sessionId());
        });
        log.info("Password changed, other sessions revoked. userId={}", userId);
    }
}
