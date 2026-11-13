package uk.huy.pathwise.planning.store;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.statestore.RedisStateStore;
import uk.huy.pathwise.planning.model.LocationSharing;

import java.time.Duration;
import java.util.Optional;

/**
 * Key {@code driver:sharing:{storeId}:{driverId}} (DATA_MODEL §3), the state as JSON.
 */
@Component
public class RedisLocationSharingStore extends RedisStateStore<String, String> implements LocationSharingStore {
    private final ObjectMapper objectMapper;

    public RedisLocationSharingStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        super(redisTemplate);
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(long storeId, long driverId, LocationSharing sharing, Duration ttl) {
        save(key(storeId, driverId), objectMapper.writeValueAsString(sharing), ttl);
    }

    @Override
    public Optional<LocationSharing> find(long storeId, long driverId) {
        return find(key(storeId, driverId)).map(json -> objectMapper.readValue(json, LocationSharing.class));
    }

    private static String key(long storeId, long driverId) {
        return "driver:sharing:" + storeId + ":" + driverId;
    }
}
