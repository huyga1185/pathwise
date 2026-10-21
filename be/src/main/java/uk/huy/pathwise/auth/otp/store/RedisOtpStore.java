package uk.huy.pathwise.auth.otp.store;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import uk.huy.pathwise.auth.infrastructure.hash.Hasher;
import uk.huy.pathwise.auth.otp.dto.Otp;
import uk.huy.pathwise.auth.otp.infrastructure.generator.OtpGenerator;
import uk.huy.pathwise.auth.otp.infrastructure.exception.OtpException;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Redis-backed implementation of the {@link OtpStore}.
 * <p>
 * Conceptually, this component acts as a highly specialized extension of the system's
 * state management layer, orchestrating the generation, storage, and verification of OTPs.
 * </p>
 * <p>
 * <b>Architectural Design Decision (Trade-off):</b> Unlike standard state stores, this
 * class intentionally does <i>not</i> implement a generic {@code StateStore} abstraction.
 * This is a deliberate architectural trade-off to strictly guarantee <b>atomicity</b>.
 * Complex OTP workflows—such as verifying max attempts, matching hashes, and deleting
 * tokens upon success—must be executed atomically to prevent race conditions and brute-force
 * replay attacks. If abstracted into a generic state store, ensuring this atomicity
 * would either require multiple separate network calls (breaking the atomic guarantee) or force
 * OTP-specific business logic (like attempt counting) to leak downwards into the Data Access
 * Object (DAO) layer. By explicitly binding to Redis and utilizing Lua scripts here, we
 * maintain strict atomicity while keeping business rules firmly in the manager layer.
 * </p>
 * <p>
 * <b>Security Posture (Fail-Closed):</b> In alignment with the system's broader state
 * management principles, this manager strictly enforces a "fail-closed" error handling
 * strategy. Any underlying infrastructure failures (such as Redis connection timeouts or
 * Lua script execution errors) are explicitly caught and translated into a generic
 * {@link StateStoreException}. This prevents technical infrastructure details from leaking
 * into the business layer and ensures that, in the event of a system outage, the
 * authentication process safely defaults to denial rather than an insecure open state.
 * </p>
 */
@Component
public class RedisOtpStore implements OtpStore {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> verifyOtpScript;
    private final Hasher hasher;
    private final OtpGenerator otpGenerator;

    /**
     * Constructs the RedisOtpStore and pre-loads the Lua script for OTP verification.
     *
     * @param redisTemplate      the Redis template for state operations
     * @param hasher             the cryptographic hasher used to secure OTPs at rest
     * @param verifyOtpLuaScript the resource containing the Lua script for atomic verification
     * @param otpGenerator       the generator for creating high-entropy raw OTPs
     * @throws IOException if the Lua script resource cannot be read
     */
    public RedisOtpStore (StringRedisTemplate redisTemplate,
                            Hasher hasher,
                            @Value("classpath:scripts/VerifyOtp.lua") Resource verifyOtpLuaScript,
                            OtpGenerator otpGenerator) throws IOException {
        this.redisTemplate = redisTemplate;
        this.verifyOtpScript = new DefaultRedisScript<>(new String(verifyOtpLuaScript.getInputStream().readAllBytes(), StandardCharsets.UTF_8), Long.class);
        this.hasher = hasher;
        this.otpGenerator = otpGenerator;
    }

    private String buildOtpKey(String purpose,
                               String identifier) {
        return String.format("otp:%s:%s", purpose, identifier);
    }

    private String buildCooldownKey(String purpose,
                                    String identifier) {
        return String.format("otp:cooldown:%s:%s", purpose, identifier);
    }

    /**
     * Generates a new OTP, applies a cooldown mechanism to prevent spam, and stores
     * the hashed OTP in Redis.
     * <p>
     * <b>Implementation Details:</b> This method enforces a 1-minute cooldown rate-limit
     * per {@code (purpose, identifier)} combination using Redis {@code SETNX} (setIfAbsent).
     * The stored OTP value is appended with an initial attempt counter (e.g., {@code "0"})
     * to track verification failures later.
     * </p>
     *
     * @param purpose    the context for the OTP (e.g., "LOGIN")
     * @param identifier the user identifier (e.g., email or phone)
     * @return the generated {@link Otp} containing the raw token for dispatch
     * @throws OtpException        if the request is made before the cooldown period expires
     * @throws StateStoreException if a Redis infrastructure error occurs
     */
    @Override
    public Otp generateAndStoreOtp(String purpose, String identifier) {
        String cooldownKey = buildCooldownKey(purpose, identifier);
        String otpKey = buildOtpKey(purpose, identifier);
        try {
            if (!redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(1)))
                throw new OtpException("Generate otp is cooling down");
            String rawOtp = otpGenerator.generateOtp(6);
            String hashedOtp = hasher.hashString(rawOtp);
            redisTemplate.opsForValue().set(otpKey, hashedOtp + "0", Expiration.seconds(300));
            return new Otp(rawOtp);
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to execute OTP verification via Redis Lua script", e);
        }
    }

    /**
     * Atomically verifies the provided raw OTP against the stored hash in Redis.
     * <p>
     * <b>Security & Atomicity:</b> This method delegates the verification logic entirely
     * to a preloaded Redis Lua script. The script atomically handles checking the TTL,
     * comparing the hash, incrementing the attempt counter, and deleting the token upon
     * successful verification or max attempts reached. This guarantees thread-safety
     * against concurrent validation requests.
     * </p>
     *
     * @param rawOtp     the plain-text OTP submitted by the user
     * @param purpose    the context for the OTP
     * @param identifier the user identifier
     * @return {@code true} if the OTP is valid and consumed; {@code false} otherwise
     * @throws StateStoreException if a Redis infrastructure error occurs
     */
    @Override
    public boolean verifyOtp(String rawOtp,
                             String purpose,
                             String identifier) {
        String otpKey = buildOtpKey(purpose, identifier);
        String hashedOtp = hasher.hashString(rawOtp);

        try {
            Long result = redisTemplate.execute(verifyOtpScript, List.of(otpKey), hashedOtp);

            return result == 1;
        } catch (DataAccessException e) {
            throw new StateStoreException("Failed to execute OTP verification via Redis Lua script", e);
        }
    }
}
