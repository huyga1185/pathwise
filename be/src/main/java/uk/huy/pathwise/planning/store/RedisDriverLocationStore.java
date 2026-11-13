package uk.huy.pathwise.planning.store;

import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.planning.model.LocationPoint;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Key {@code driver:location:{storeId}:{driverId}} (DATA_MODEL §3), a hash of the point's {@code recordedAt} and the
 * point as JSON. Comparing and writing happen in {@code scripts/SaveLatestLocation.lua}, so two batches arriving at
 * the same time cannot replace a newer point with an older one.
 */
@Component
public class RedisDriverLocationStore implements DriverLocationStore {
    private static final DefaultRedisScript<Long> SAVE_LATEST = loadScript("scripts/SaveLatestLocation.lua");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisDriverLocationStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean saveIfNewer(long storeId, long driverId, LocationPoint point, Duration ttl) {
        String key = key(storeId, driverId);
        try {
            Long saved = redisTemplate.execute(SAVE_LATEST, List.of(key),
                    String.valueOf(point.recordedAt().toEpochMilli()),
                    objectMapper.writeValueAsString(point),
                    String.valueOf(Math.max(1, ttl.toMillis())));
            return saved != null && saved == 1;
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to save the location in Redis for key: " + key, e);
        }
    }

    @Override
    public Optional<LocationPoint> find(long storeId, long driverId) {
        String key = key(storeId, driverId);
        try {
            Object json = redisTemplate.opsForHash().get(key, "v");
            return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json.toString(), LocationPoint.class));
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to read the location from Redis for key: " + key, e);
        }
    }

    private static String key(long storeId, long driverId) {
        return "driver:location:" + storeId + ":" + driverId;
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
