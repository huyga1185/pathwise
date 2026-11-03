package uk.huy.pathwise.core.ratelimit;

import java.time.Duration;

/**
 * Result of {@link RateLimiter#tryAcquire}.
 */
public sealed interface RateLimitDecision {
    record Allowed() implements RateLimitDecision {}

    /**
     * @param retryAfter time left until the window ends
     */
    record Limited(Duration retryAfter) implements RateLimitDecision {}
}
