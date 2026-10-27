package uk.huy.pathwise.routing.infrastructure.throttle;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * One call per interval ({@code 1 s / perSecond}), slots handed out in arrival order.
 * <p>
 * Each caller makes one atomic reservation with {@code scripts/ReserveThrottleSlot.lua}: the script reads the time
 * the next slot becomes free, gives that slot to the caller, moves the "next free" time one interval later and
 * returns how long the caller must wait. The caller then sleeps exactly that long, once. Callers therefore queue up
 * in the order they reach Redis instead of polling and racing for a slot that just became free.
 * <p>
 * Technical trade-off: the reservation has to read, compare and write in one step, otherwise two callers could get
 * the same slot; Redis runs a Lua script atomically in one round trip, where doing it in Java would need WATCH/MULTI
 * with retries. The script uses the Redis clock, so instances with skewed clocks still share one schedule. The cost:
 * the logic is tied to Redis and only covered by integration tests (Testcontainers). A caller that would wait longer
 * than {@code maxWait} reserves nothing, so a rejected request never delays the ones after it.
 */
public class RedisRequestThrottle implements RequestThrottle {
    private static final DefaultRedisScript<Long> RESERVE_SLOT = loadScript("scripts/ReserveThrottleSlot.lua");

    private final StringRedisTemplate redisTemplate;
    private final String key;
    private final Duration interval;
    private final Duration maxWait;

    public RedisRequestThrottle(StringRedisTemplate redisTemplate, String key, int perSecond, Duration maxWait) {
        if (perSecond <= 0) throw new IllegalArgumentException("perSecond must be positive");
        this.redisTemplate = redisTemplate;
        this.key = key;
        this.interval = Duration.ofMillis(Math.max(1, 1000 / perSecond));
        this.maxWait = maxWait;
    }

    @Override
    public boolean acquire() {
        long waitMillis = reserve();
        if (waitMillis < 0) return false;
        if (waitMillis == 0) return true;
        try {
            Thread.sleep(waitMillis);
            return true;
        } catch (InterruptedException e) {
            // The reserved slot is simply left unused.
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private long reserve() {
        try {
            Long wait = redisTemplate.execute(RESERVE_SLOT, List.of(key),
                    String.valueOf(interval.toMillis()), String.valueOf(maxWait.toMillis()));
            if (wait == null) throw new StateStoreException("Rate limit script returned no result");
            return wait;
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to reserve a rate limit slot in Redis", e);
        }
    }

    private static DefaultRedisScript<Long> loadScript(String path) {
        try {
            String source = new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
            return new DefaultRedisScript<>(source, Long.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + path, e);
        }
    }
}
