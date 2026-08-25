package uk.huy.pathwise.auth.token.jwt.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.util.Map;
import java.util.Optional;

@Service
public class AccessTokenService {
    private final JwtTokenGenerator jwtTokenGenerator;
    public AccessTokenService(@Qualifier("hmacAccessTokenGenerator") JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    public String generateAccessToken(UserIdentity identity) {
        if (identity == null) throw new NullPointerException("UserIdentity could not be null");
        return jwtTokenGenerator.generateJwt(String.valueOf(identity.id()),
                null,
                null,
                null,
                Map.of("role", identity.role(),
                        "email", identity.email()));
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
