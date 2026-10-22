package uk.huy.pathwise.auth.token.jwt.infrastructure.generator;

import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface JwtTokenGenerator {


    String generateJwt(String subject,
                                 List<String> audience,
                                 String jti,
                                 Duration nbf,
                                 Map<String, Object> claims);

    Optional<JwtClaimsSet> verifyJwt(String jwt, String expectedAudience);
    Duration getExpirationTime();
}
