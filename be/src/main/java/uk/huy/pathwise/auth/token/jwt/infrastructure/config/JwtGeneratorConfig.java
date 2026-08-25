package uk.huy.pathwise.auth.token.jwt.infrastructure.config;

import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.hmacgenerator.HMACJwtTokenGenerator;

import java.time.Duration;

@Configuration
public class JwtGeneratorConfig {
    @Bean
    public JwtTokenGenerator hmacAccessTokenGenerator(@Value("${token.jwt.access-token-secret}") String secretKey,
                                                  @Value("${token.jwt.access-token-hasing-algorithm:HS256}") String algorithm,
                                                  @Value("${token.jwt.access-token-expiration-time:14m}") Duration expirationTime,
                                                  @Value("${token.jwt.access-token-issuer:#{null}}") String issuer) {
        return new HMACJwtTokenGenerator(JWSAlgorithm.parse(algorithm),
                secretKey,
                expirationTime,
                issuer);
    }

    @Bean
    public JwtTokenGenerator hmacOtpTokenGenerator(@Value("${token.jwt.otp-token-secret}") String secretKey,
                                               @Value("${token.jwt.otp-token-hasing-algorithm:HS256}") String algorithm,
                                               @Value("${token.jwt.otp-token-expiration-time:14m}") Duration expirationTime,
                                               @Value("${token.jwt.otp-token-issuer:#{null}}") String issuer) {
        return new HMACJwtTokenGenerator(JWSAlgorithm.parse(algorithm),
                secretKey,
                expirationTime,
                issuer);
    }
}
