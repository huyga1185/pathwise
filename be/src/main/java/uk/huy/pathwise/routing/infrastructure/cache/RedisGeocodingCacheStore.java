package uk.huy.pathwise.routing.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.statestore.RedisStateStore;

/**
 * Fail-closed like every Redis store: a Redis outage is an error, not a cache miss, because the rate limiter that
 * protects Nominatim needs Redis as well.
 */
@Component
public class RedisGeocodingCacheStore extends RedisStateStore<String, String> implements GeocodingCacheStore {
    public RedisGeocodingCacheStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }
}
