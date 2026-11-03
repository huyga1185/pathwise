package uk.huy.pathwise.core.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * {@link RateLimiter} in Redis: each {@code (rule, subject)} pair may make {@code limit} requests per window, the window
 * starting with its first request. Counting and setting the expiry is one atomic step
 * ({@code scripts/FixedWindowRateLimit.lua}), so a counter never lives without a TTL. Subjects are hashed in the key
 * ({@code ratelimit:{rule}:{sha256(subject)}}), so Redis holds no email address.
 */
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(RateLimitProperties.class)
public class RedisRateLimiter implements RateLimiter {
    private static final DefaultRedisScript<List> FIXED_WINDOW = loadScript("scripts/FixedWindowRateLimit.lua");
    private static final String KEY_PREFIX = "ratelimit:";

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;
    private final Hasher hasher;

    @Override
    public RateLimitDecision tryAcquire(String rule, String subject) {
        if (subject == null) throw new NullPointerException("Rate limit subject could not be null");
        RateLimitProperties.Limit limit = properties.rules().get(rule);
        if (limit == null) throw new IllegalStateException("No rate-limit.rules." + rule + " configured");
        String key = KEY_PREFIX + rule + ":" + hasher.hashString(subject);
        List<?> result;
        try {
            result = redisTemplate.execute(FIXED_WINDOW, List.of(key), String.valueOf(limit.window().toMillis()));
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to count a request for rate limit " + rule + " in Redis", e);
        }
        if (result == null || result.size() != 2) throw new StateStoreException("Rate limit script returned " + result);
        long count = ((Number) result.get(0)).longValue();
        if (count <= limit.limit()) return new RateLimitDecision.Allowed();
        return new RateLimitDecision.Limited(Duration.ofMillis(((Number) result.get(1)).longValue()));
    }

    private static DefaultRedisScript<List> loadScript(String path) {
        try {
            String source = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
            return new DefaultRedisScript<>(source, List.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
    }
}
