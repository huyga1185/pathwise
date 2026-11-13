package uk.huy.pathwise.core.housekeeping;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * {@code SET housekeeping:lock <token> NX PX <ttl>}: the key always has a TTL, so a crashed run frees it.
 */
@Component
@RequiredArgsConstructor
class RedisHousekeepingLock implements HousekeepingLock {
    static final String KEY = "housekeeping:lock";
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String tryAcquire(Duration ttl) {
        String token = UUID.randomUUID().toString();
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(KEY, token, ttl)) ? token : null;
    }

    @Override
    public void release(String token) {
        redisTemplate.execute(RELEASE, List.of(KEY), token);
    }
}
