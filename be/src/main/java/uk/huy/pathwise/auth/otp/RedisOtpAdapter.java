package uk.huy.pathwise.auth.otp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import uk.huy.pathwise.auth.infastructure.hash.Hasher;
import uk.huy.pathwise.auth.otp.infrastructure.generator.OtpGenerator;
import uk.huy.pathwise.auth.otp.infrastructure.exception.OtpException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Component
public class RedisOtpAdapter implements OtpService {
    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> verifyOtpScript;
    private final Hasher hasher;
    private final OtpGenerator otpGenerator;

    public RedisOtpAdapter(StringRedisTemplate redisTemplate,
                      Hasher hasher,
                      @Value("classpath:scripts/VerifyOtp.lua") Resource verifyOtpLuaScript,
                      OtpGenerator otpGenerator) throws IOException {
        this.redisTemplate = redisTemplate;
        this.verifyOtpScript = new DefaultRedisScript<>(new String(verifyOtpLuaScript.getInputStream().readAllBytes(), StandardCharsets.UTF_8), Long.class);
        this.hasher = hasher;
        this.otpGenerator = otpGenerator;
    }

    private void validatePurposeAndIdentifier(String purpose, String identifier) {
        if (purpose == null) throw new NullPointerException("Purpose could not be null");
        if (identifier == null) throw new NullPointerException("Identifier could not be null");
        if (purpose.isEmpty()) throw new IllegalArgumentException("Purpose could not be empty");
        if (identifier.isEmpty()) throw new IllegalArgumentException("Identifier could not be empty");
    }

    private String buildOtpKey(String purpose,
                               String identifier) {
        validatePurposeAndIdentifier(purpose, identifier);
        return String.format("otp:%s:%s", purpose, identifier);
    }

    private String buildCooldownKey(String purpose,
                                    String identifier) {
        validatePurposeAndIdentifier(purpose, identifier);
        return String.format("otp:cooldown:%s:%s", purpose, identifier);
    }

    @Override
    public Optional<String> generateAndStoreOtp(String purpose,
                                                String identifier) {
        String cooldownKey = buildCooldownKey(purpose, identifier);
        String otpKey = buildOtpKey(purpose, identifier);
        if (!redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", Duration.ofMinutes(1)))
            throw new OtpException("Generate otp is cooling down");
        String rawOtp = otpGenerator.generateOtp(6);
        String hashedOtp = hasher.hashString(rawOtp);
        redisTemplate.opsForValue().set(otpKey, hashedOtp + "0", Expiration.seconds(300));
        return Optional.of(rawOtp);
    }

    @Override
    public boolean verifyOtp(String rawOtp,
                             String purpose,
                             String identifier) {
        if (rawOtp == null) throw new NullPointerException("Otp could not be empty");
        if (rawOtp.isEmpty()) throw new IllegalArgumentException("Otp could not be empty");
        String otpKey = buildOtpKey(purpose, identifier);
        String hashedOtp = hasher.hashString(rawOtp);
        Long result = redisTemplate.execute(verifyOtpScript, List.of(otpKey), hashedOtp);
        return result == 1;
    }
}
