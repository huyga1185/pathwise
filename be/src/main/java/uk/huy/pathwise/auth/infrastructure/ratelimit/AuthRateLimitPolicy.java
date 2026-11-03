package uk.huy.pathwise.auth.infrastructure.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimiter;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Which {@code rate-limit.rules} each auth endpoint counts, and by what (P-16, P-37): one place for the rule names.
 * Every rule of an endpoint is counted until one is limited; the services turn the decision into the response.
 * <p>
 * The IP is the one of {@code ClientDetail}, which trusts {@code X-Forwarded-For} until T41 (F-21): a client that
 * forges the header escapes the limit by IP, not the one by email. </p>
 * Throws {@link StateStoreException} from every method if the counters cannot be reached
 */
@Component
@RequiredArgsConstructor
public class AuthRateLimitPolicy {
    private final RateLimiter rateLimiter;

    public RateLimitDecision logIn(String ip, String email) {
        return firstLimited(rateLimiter.tryAcquire("log-in-ip", ip), () -> rateLimiter.tryAcquire("log-in-email", normalize(email)));
    }

    public RateLimitDecision register(String ip) {
        return rateLimiter.tryAcquire("register-ip", ip);
    }

    public RateLimitDecision resendVerification(String ip, String email) {
        return firstLimited(rateLimiter.tryAcquire("resend-verification-ip", ip),
                () -> rateLimiter.tryAcquire("resend-verification-email", normalize(email)));
    }

    public RateLimitDecision forgotPassword(String ip, String email) {
        return firstLimited(rateLimiter.tryAcquire("forgot-password-ip", ip),
                () -> rateLimiter.tryAcquire("forgot-password-email", normalize(email)));
    }

    public RateLimitDecision changePasswordOtp(long userId) {
        return rateLimiter.tryAcquire("change-password-otp-user", String.valueOf(userId));
    }

    private static RateLimitDecision firstLimited(RateLimitDecision first, Supplier<RateLimitDecision> second) {
        return first instanceof RateLimitDecision.Limited ? first : second.get();
    }

    // Same normalization as the account lookup (P-19), so case variants share one counter.
    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
