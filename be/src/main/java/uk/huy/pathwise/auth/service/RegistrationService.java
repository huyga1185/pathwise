package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.request.ResendVerificationRequest;
import uk.huy.pathwise.auth.dto.request.VerifyEmailRequest;
import uk.huy.pathwise.auth.dto.response.RegisterResponse;
import uk.huy.pathwise.auth.infrastructure.ratelimit.AuthRateLimitPolicy;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimitedException;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.service.StoreProvisioningService;
import uk.huy.pathwise.user.dto.AccountState;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.function.Supplier;

/**
 * OWNER sign-up and email verification (D-15, FR-A01, FR-A02).
 * <p>
 * Registration creates the store and its OWNER in one transaction and sends the {@code VERIFY_EMAIL} OTP only after
 * the commit, outside the transaction (ARCHITECTURE.md §7). If the email cannot be sent the account is kept and the
 * user asks for a new code with {@code /email/verify/resend}; the OTP cooldown was released, so that works at once.
 */
@Service
@Slf4j
public class RegistrationService {
    private final UserAccountService userService;
    private final StoreProvisioningService storeProvisioningService;
    private final OtpManager otpManager;
    private final TransactionTemplate transactionTemplate;
    private final AuthRateLimitPolicy rateLimitPolicy;

    public RegistrationService(UserAccountService userService,
                               StoreProvisioningService storeProvisioningService,
                               OtpManager otpManager,
                               PlatformTransactionManager transactionManager,
                               AuthRateLimitPolicy rateLimitPolicy) {
        this.userService = userService;
        this.storeProvisioningService = storeProvisioningService;
        this.otpManager = otpManager;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.rateLimitPolicy = rateLimitPolicy;
    }

    /**
     * @throws AppException {@code EMAIL_EXISTED} if the email (in any case) is already used; {@code RATE_LIMITED} by IP
     */
    public RegisterResponse register(ClientDetail clientDetail, RegisterRequest request) {
        if (request == null) throw new NullPointerException("Register request could not be null");
        enforce(() -> rateLimitPolicy.register(clientDetail.ip()));
        // Programmatic transaction: the OTP email must be sent after the commit, from this same method.
        UserIdentity owner = transactionTemplate.execute(status -> {
            long storeId = storeProvisioningService.createStore(request.storeName().trim());
            return userService.createOwner(request.email(), request.password(), blankToNull(request.fullName()), storeId);
        });
        sendVerificationOtp(owner.id(), owner.email());
        return new RegisterResponse(owner.email(), UserStatus.PENDING_VERIFICATION);
    }

    /**
     * Activates the account if {@code otp} is its current {@code VERIFY_EMAIL} OTP.
     *
     * @throws AppException {@code INVALID_OTP} for a wrong or expired OTP, or an unknown email (not revealed);
     *                      {@code EMAIL_ALREADY_VERIFIED} if there is nothing left to verify
     */
    public void verifyEmail(VerifyEmailRequest request) {
        if (request == null) throw new NullPointerException("Verify email request could not be null");
        AccountState account = userService.findAccountState(request.email())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_OTP));
        if (account.emailVerified()) throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        if (!otpManager.verifyOtp(OtpPurpose.VERIFY_EMAIL, account.userId(), request.otp()))
            throw new AppException(ErrorCode.INVALID_OTP);
        userService.markEmailVerified(account.userId());
    }

    /**
     * Sends a new code when the account is waiting for verification. Always succeeds, so the response does not
     * reveal whether the email is registered; a running cooldown or a failed email is only logged.
     *
     * @throws AppException {@code RATE_LIMITED} by IP or email, registered or not
     */
    public void resendVerification(ClientDetail clientDetail, ResendVerificationRequest request) {
        if (request == null) throw new NullPointerException("Resend verification request could not be null");
        enforce(() -> rateLimitPolicy.resendVerification(clientDetail.ip(), request.email()));
        userService.findAccountState(request.email())
                .filter(account -> !account.emailVerified() && account.status() == UserStatus.PENDING_VERIFICATION)
                .ifPresent(account -> sendVerificationOtp(account.userId(), account.email()));
    }

    private void sendVerificationOtp(long userId, String email) {
        try {
            if (otpManager.sendOtp(OtpPurpose.VERIFY_EMAIL, userId, email) == OtpSendResult.COOLDOWN)
                log.info("Verification OTP not sent, cooldown running. userId={}", userId);
        } catch (EmailDeliveryException e) {
            // OtpManager already released the cooldown, so the user can ask again right away.
            log.warn("Verification email not sent. userId={}", userId);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Fails closed: a limit that cannot be checked refuses the request (NFR-01, P-37).
     *
     * @throws RateLimitedException if a limit of the endpoint is used up
     * @throws AppException         {@code SERVICE_UNAVAILABLE} if the counters cannot be reached
     */
    private static void enforce(Supplier<RateLimitDecision> rateLimit) {
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
