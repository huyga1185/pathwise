package uk.huy.pathwise.auth.token.jwt.manager;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.token.jwt.dto.AccessTokenVerification;
import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtRejectReason;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtVerification;
import uk.huy.pathwise.auth.token.jwt.infrastructure.exception.JwtException;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
public class AccessTokenManager {
    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";
    private static final String SESSION_CLAIM = "sid";

    private final JwtTokenGenerator jwtTokenGenerator;

    public AccessTokenManager(@Qualifier("hmacAccessTokenGenerator") JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    /**
     * @param sessionId refresh token family the access token belongs to (claim {@code sid}, P-07)
     */
    public IssuedAccessToken generateAccessToken(UserIdentity identity, String sessionId) {
        if (identity == null) throw new NullPointerException("UserIdentity could not be null");
        if (sessionId == null) throw new NullPointerException("Session id could not be null");
        // Taken before signing and truncated like the JWT "exp" claim, so it is never later than the real expiry.
        Instant expiresAt = Instant.now().plus(jwtTokenGenerator.getExpirationTime()).truncatedTo(ChronoUnit.SECONDS);
        String token = jwtTokenGenerator.generateJwt(String.valueOf(identity.id()),
                null,
                null,
                null,
                Map.of(ROLE_CLAIM, identity.role(),
                        EMAIL_CLAIM, identity.email(),
                        SESSION_CLAIM, sessionId));
        return new IssuedAccessToken(token, expiresAt);
    }

    public AccessTokenVerification verifyAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank())
            return new AccessTokenVerification.Rejected(JwtRejectReason.MALFORMED);
        return switch (jwtTokenGenerator.verifyJwt(accessToken, null)) {
            case JwtVerification.Verified verified -> toAuthenticated(verified.claims());
            case JwtVerification.Rejected rejected -> new AccessTokenVerification.Rejected(rejected.reason());
        };
    }

    private static AccessTokenVerification toAuthenticated(JwtClaimsSet claims) {
        try {
            UserIdentity identity = new UserIdentity(Long.parseLong(claims.sub()),
                    claims.getClaimAsString(EMAIL_CLAIM),
                    UserRole.valueOf(claims.getClaimAsString(ROLE_CLAIM)));
            return new AccessTokenVerification.Authenticated(
                    new AuthenticatedUser(identity, claims.getClaimAsString(SESSION_CLAIM)));
        } catch (IllegalArgumentException | NullPointerException | JwtException e) {
            // Correctly signed but not shaped like our access tokens (e.g. unknown role, non-string claim).
            return new AccessTokenVerification.Rejected(JwtRejectReason.INVALID_CLAIMS);
        }
    }
}
