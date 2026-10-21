package uk.huy.pathwise.core.statestore;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

/**
 * An abstract base implementation of {@link StateStore} using Redis.
 * <p>
 * <b>Design Decision (Fail-Closed by Default):</b> All data access operations in this
 * base class are wrapped in a try-catch block. Any underlying Spring {@link DataAccessException}
 * (e.g., Redis connection failure) is translated into a generic {@link StateStoreException}.
 * This ensures that infrastructure details do not leak into the business layer and guarantees
 * a "fail-closed" security posture by default.
 * </p>
 * <p>
 * Subclasses or utilizing services that require a "fail-open" behavior (e.g., ignoring cache
 * misses if Redis is down) should explicitly override these methods or catch the
 * {@code StateStoreException} to implement fallback logic.
 * </p>
 *
 * @param <K> the type of keys maintained by this store
 * @param <V> the type of mapped values
 */
public abstract class RedisStateStore<K, V> implements StateStore<K, V> {
    protected final RedisTemplate<K, V> redisTemplate;

    public RedisStateStore(RedisTemplate<K, V> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(K key, V value, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (DataAccessException e) {
            throw new StateStoreException(String.format("Failed to save state to Redis for key: %s", key), e);
        }
    }

    @Override
    public Optional<V> find(K key) {
        try {
            V val = redisTemplate.opsForValue().get(key);
            return Optional.ofNullable(val); // Rút gọn val == null ? Optional.empty() : Optional.of(val)
        } catch (DataAccessException e) {
            throw new StateStoreException(String.format("Failed to retrieve state from Redis for key: %s", key), e);
        }
    }

    @Override
    public void delete(K key) {
        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            throw new StateStoreException(String.format("Failed to delete state in Redis for key: %s", key), e);
        }
    }
}
