package uk.huy.pathwise.core.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/**
 * {@code rate-limit.rules.<rule>.*} (P-16): every rule a module checks must be configured, there is no default.
 */
@ConfigurationProperties("rate-limit")
public record RateLimitProperties(Map<String, Limit> rules) {
    public RateLimitProperties {
        rules = rules == null ? Map.of() : Map.copyOf(rules);
    }

    /**
     * @param limit  requests allowed per window
     * @param window length of the fixed window, starting with its first request
     */
    public record Limit(int limit, Duration window) {}
}
