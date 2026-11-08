package uk.huy.pathwise.core.ratelimit;

import uk.huy.pathwise.core.statestore.StateStoreException;

/**
 * Counts requests per {@code (rule, subject)} in a fixed window (P-16, P-37). Limits are configured per rule in
 * {@code rate-limit.rules.<rule>}. Callers decide what a {@link RateLimitDecision.Limited} means for their endpoint.
 */
public interface RateLimiter {
    /**
     * Counts one request of {@code subject} under {@code rule}, whether or not it is allowed.
     *
     * @param subject who is counted: an IP, a normalized email, a user or store id
     * @throws StateStoreException   if the counter cannot be read or written; callers fail closed
     * @throws IllegalStateException if the rule is not configured
     */
    RateLimitDecision tryAcquire(String rule, String subject);
}
