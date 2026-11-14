package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.infrastructure.ratelimit.AuthRateLimitPolicy;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.resetlink.ResetLinkProperties;
import uk.huy.pathwise.auth.resetlink.ResetLinkStore;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimitedException;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.notification.service.EmailNotificationService;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.AccountState;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Emails an ADMIN sends for a user (T57, FR-A13, P-63). Nothing here is returned to the ADMIN: codes and links only
 * go to the user's mailbox. A failed email throws, so the ADMIN knows it was not sent.
 */
@Slf4j
@Service
@EnableConfigurationProperties(ResetLinkProperties.class)
public class AccountSupportService implements AccountSupport {
    private final UserAccountService userService;
    private final OtpManager otpManager;
    private final AuthRateLimitPolicy rateLimitPolicy;
    private final ResetLinkStore resetLinkStore;
    private final ResetLinkProperties resetLink;
    private final OpaqueTokenGenerator tokenGenerator;
    private final Hasher hasher;
    private final EmailNotificationService emailNotificationService;
    private final String appUrl;

    public AccountSupportService(UserAccountService userService,
                                 OtpManager otpManager,
                                 AuthRateLimitPolicy rateLimitPolicy,
                                 ResetLinkStore resetLinkStore,
                                 ResetLinkProperties resetLink,
                                 OpaqueTokenGenerator tokenGenerator,
                                 Hasher hasher,
                                 EmailNotificationService emailNotificationService) {
        this.userService = userService;
        this.otpManager = otpManager;
        this.rateLimitPolicy = rateLimitPolicy;
        this.resetLinkStore = resetLinkStore;
        this.resetLink = resetLink;
        this.tokenGenerator = tokenGenerator;
        this.hasher = hasher;
        this.emailNotificationService = emailNotificationService;
        this.appUrl = emailNotificationService.appUrl();
    }

    @Override
    public void sendVerificationEmail(long userId) {
        AccountState account = accountOf(userId);
        if (account.emailVerified()) throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        if (account.status() == UserStatus.DISABLED) throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        enforce(() -> rateLimitPolicy.supportVerificationEmail(account.email()));
        if (otpManager.sendOtp(OtpPurpose.VERIFY_EMAIL, userId, account.email()) == OtpSendResult.COOLDOWN)
            throw new AppException(ErrorCode.OTP_COOLDOWN);
        log.info("Verification email resent by an ADMIN. userId={}", userId);
    }

    @Override
    public void sendPasswordResetLink(long userId) {
        AccountState account = accountOf(userId);
        if (account.status() == UserStatus.DISABLED) throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        if (account.status() != UserStatus.ACTIVE) throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        enforce(() -> rateLimitPolicy.supportPasswordReset(account.email()));
        String token = tokenGenerator.generateOpaqueToken();
        String tokenHash = hasher.hashString(token);
        resetLinkStore.save(userId, tokenHash, resetLink.ttl());
        try {
            emailNotificationService.send(EmailNotificationService.RESET_LINK, account.email(), Map.of(
                    "link", appUrl.replaceAll("/+$", "") + resetLink.path() + "?token=" + token,
                    "validHours", resetLink.ttl().toHours()));
        } catch (RuntimeException e) {
            // A link nobody received must not stay usable.
            resetLinkStore.delete(userId, tokenHash);
            throw e;
        }
        log.info("Password reset link sent by an ADMIN. userId={}", userId);
    }

    private AccountState accountOf(long userId) {
        String email = userService.findUserById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)).email();
        return userService.findAccountState(email).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    /**
     * Fails closed (NFR-01, P-37).
     */
    static void enforce(Supplier<RateLimitDecision> rateLimit) {
        RateLimitDecision decision;
        try {
            decision = rateLimit.get();
        } catch (StateStoreException e) {
            log.error("Rate limit could not be checked, refusing the request", e);
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        switch (decision) {
            case RateLimitDecision.Allowed allowed -> {}
            case RateLimitDecision.Limited limited -> throw new RateLimitedException(limited.retryAfter());
        }
    }
}
