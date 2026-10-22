package uk.huy.pathwise.auth.token.opaque.otptoken.statestore;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.statestore.RedisStateStore;
import uk.huy.pathwise.core.statestore.StateStoreException;

/**
 * Redis-backed implementation of the {@link OtpTokenStore}.
 * <p>
 * This component leverages Redis to securely manage OTP tokens. It ensures that
 * critical operations, specifically the one-time retrieval of tokens, are executed
 * atomically at the data store level. By extending {@link RedisStateStore}, it also
 * inherits the default "fail-closed" error handling mechanism.
 * </p>
 */
@Component
public class RedisOtpTokenStore extends RedisStateStore<String, String> implements OtpTokenStore {

    /**
     * Constructs a new {@code RedisOtpTokenStore} with the provided Redis template.
     *
     * @param redisTemplate the template used for executing Redis operations
     */
    public RedisOtpTokenStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    /**
     * Atomically retrieves and deletes the OTP token associated with the given key from Redis.
     * <p>
     * <b>Implementation Note:</b> This method utilizes the Redis {@code GETDEL} command
     * (via Spring Data Redis) to guarantee atomicity. This prevents race conditions
     * where multiple concurrent requests might otherwise successfully retrieve the same OTP.
     * </p>
     *
     * @param key the key whose associated OTP token is to be retrieved and removed
     * @return the OTP token string if it exists, or {@code null} if the key is not found
     * @throws StateStoreException if an underlying infrastructure error occurs
     *         (e.g., Redis connection failure or timeout), wrapping the original
     *         {@link DataAccessException} to preserve the root cause.
     */
    @Override
    public String getAndDelete(String key) {
        try {
            return redisTemplate.opsForValue().getAndDelete(key);
        } catch (DataAccessException e) {
            throw new StateStoreException(String.format("Failed to execute getAndDelete in Redis for key: %s", key), e);
        }
    }
}
