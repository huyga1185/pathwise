package uk.huy.pathwise.auth.otp.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Redis implementation of {@link OtpStore}.
 * <p>
 * It does not build on the generic {@code StateStore} on purpose: verification (compare hash, count attempts,
 * delete) must be one atomic step, which needs a Lua script bound to Redis. Every Redis failure becomes a
 * {@link StateStoreException}, so authentication fails closed.
 * <p>
 * Technical trade-off (ARCHITECTURE.md §3, §7): the attempt limit is enforced inside {@code scripts/VerifyOtp.lua},
 * so a small piece of OTP logic lives in the store instead of {@code OtpManager}. Doing the check in Java would
 * need several round trips (GET, then SET or DEL) with a race between them, or WATCH/MULTI with retries. Redis
 * runs a Lua script as one atomic step in one round trip. The cost: the logic is tied to Redis and the script is
 * only covered by integration tests (Testcontainers). The limit itself ({@code maxAttempts}) still comes from
 * {@code OtpManager}; the script reads no configuration.
 * <p>
 * Keys: {@code otp:{purpose}:{userId}} → {@code {sha256}{attempts}}, {@code otp:cooldown:{purpose}:{userId}} → {@code 1}.
 */
@Component
public class RedisOtpStore implements OtpStore {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> verifyOtpScript;

    public RedisOtpStore(StringRedisTemplate redisTemplate,
                         @Value("classpath:scripts/VerifyOtp.lua") Resource verifyOtpLuaScript) throws IOException {
        this.redisTemplate = redisTemplate;
        this.verifyOtpScript = new DefaultRedisScript<>(
                new String(verifyOtpLuaScript.getInputStream().readAllBytes(), StandardCharsets.UTF_8), Long.class);
    }

    static String otpKey(OtpPurpose purpose, long userId) {
        return "otp:" + purpose.name() + ":" + userId;
    }

    static String cooldownKey(OtpPurpose purpose, long userId) {
        return "otp:cooldown:" + purpose.name() + ":" + userId;
    }

    @Override
    public boolean tryStartCooldown(OtpPurpose purpose, long userId, Duration cooldown) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(cooldownKey(purpose, userId), "1", cooldown));
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to start OTP cooldown in Redis", e);
        }
    }

    @Override
    public void save(OtpPurpose purpose, long userId, String hashedOtp, Duration ttl) {
        try {
            redisTemplate.opsForValue().set(otpKey(purpose, userId), hashedOtp + "0", ttl);
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to store OTP in Redis", e);
        }
    }

    /**
     * Runs {@code VerifyOtp.lua}, which reads, compares, counts and deletes in one atomic step. Without that,
     * concurrent calls could each see the same attempt count and allow more than {@code maxAttempts} guesses, or
     * consume the same OTP twice.
     */
    @Override
    public boolean verify(OtpPurpose purpose, long userId, String hashedOtp, int maxAttempts) {
        try {
            Long result = redisTemplate.execute(verifyOtpScript, List.of(otpKey(purpose, userId)),
                    hashedOtp, String.valueOf(maxAttempts));
            return result != null && result == 1;
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to verify OTP via Redis Lua script", e);
        }
    }

    @Override
    public void discard(OtpPurpose purpose, long userId) {
        try {
            redisTemplate.delete(List.of(otpKey(purpose, userId), cooldownKey(purpose, userId)));
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to discard OTP in Redis", e);
        }
    }
}
