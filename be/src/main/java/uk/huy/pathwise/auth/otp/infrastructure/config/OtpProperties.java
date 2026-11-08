package uk.huy.pathwise.auth.otp.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code otp.*} (F-09): one source for the OTP lifetime used both in Redis and in the email text.
 *
 * @param ttl         lifetime of an OTP ({@code otp.ttl}, env {@code OTP_TTL})
 * @param cooldown    minimum delay between two OTPs of the same purpose for the same user
 * @param maxAttempts wrong attempts after which the OTP is deleted
 * @param length      number of digits
 */
@ConfigurationProperties("otp")
public record OtpProperties(@DefaultValue("5m") Duration ttl,
                            @DefaultValue("60s") Duration cooldown,
                            @DefaultValue("3") int maxAttempts,
                            @DefaultValue("6") int length) {}
