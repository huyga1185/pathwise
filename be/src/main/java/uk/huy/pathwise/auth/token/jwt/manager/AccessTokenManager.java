package uk.huy.pathwise.auth.token.jwt.manager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Component
public class AccessTokenManager {
    private final JwtTokenGenerator jwtTokenGenerator;

    public AccessTokenManager(@Qualifier("hmacAccessTokenGenerator") JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    public IssuedAccessToken generateAccessToken(UserIdentity identity) {
        if (identity == null) throw new NullPointerException("UserIdentity could not be null");
        // Taken before signing and truncated like the JWT "exp" claim, so it is never later than the real expiry.
        Instant expiresAt = Instant.now().plus(jwtTokenGenerator.getExpirationTime()).truncatedTo(ChronoUnit.SECONDS);
        String token = jwtTokenGenerator.generateJwt(String.valueOf(identity.id()),
                null,
                null,
                null,
                Map.of("role", identity.role(),
                        "email", identity.email()));
        return new IssuedAccessToken(token, expiresAt);
    }

    public Optional<UserIdentity> verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) return Optional.empty();
        Optional<JwtClaimsSet> jwtClaimsSet = jwtTokenGenerator.verifyJwt(accessToken, null);
        return jwtClaimsSet.map(claimsSet ->
                new UserIdentity(Long.parseLong(claimsSet.sub()),
                        claimsSet.getClaimAsString("email"),
                        UserRole.valueOf(claimsSet.getClaimAsString("role"))));
    }
}
