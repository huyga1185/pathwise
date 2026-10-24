package uk.huy.pathwise.auth.token.jwt.infrastructure.generator;

import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtVerification;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface JwtTokenGenerator {


    String generateJwt(String subject,
                                 List<String> audience,
                                 String jti,
                                 Duration nbf,
                                 Map<String, Object> claims);

    /**
     * @throws uk.huy.pathwise.auth.token.jwt.infrastructure.exception.JwtException if verification itself fails (crypto/configuration)
     */
    JwtVerification verifyJwt(String jwt, String expectedAudience);
    Duration getExpirationTime();
}
