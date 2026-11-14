package uk.huy.pathwise.admin.infrastructure;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.statestore.RedisStateStore;

import java.time.Duration;
import java.util.Optional;

/**
 * Key {@code metrics:business:{from}:{to}} (DATA_MODEL §3), the response as JSON, with a TTL.
 */
@Component
public class RedisMetricsCacheStore extends RedisStateStore<String, String> implements MetricsCacheStore {
    public RedisMetricsCacheStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public Optional<String> find(String key) {
        return super.find(key);
    }

    @Override
    public void save(String key, String json, Duration ttl) {
        super.save(key, json, ttl);
    }
}
