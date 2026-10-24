package uk.huy.pathwise.auth.token.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.hmacgenerator.HMACJwtTokenGenerator;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.jwt.dto.AccessTokenVerification;
import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtRejectReason;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenManagerTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private final AccessTokenManager accessTokenManager = new AccessTokenManager(
            new HMACJwtTokenGenerator(JWSAlgorithm.HS256, SECRET, Duration.ofMinutes(14), null));

    @Test
    void verifyAccessToken_whenTokenGenerated_returnSameIdentityAndSession() {
        UserIdentity identity = new UserIdentity(7L, "admin@pathwise.test", UserRole.ADMIN);

        String token = accessTokenManager.generateAccessToken(identity, "session-7").token();

        assertThat(accessTokenManager.verifyAccessToken(token))
                .isEqualTo(new AccessTokenVerification.Authenticated(new AuthenticatedUser(identity, "session-7")));
    }

    @Test
    void verifyAccessToken_whenTokenSignedWithAnotherSecret_rejectBadSignature() {
        AccessTokenManager other = new AccessTokenManager(new HMACJwtTokenGenerator(JWSAlgorithm.HS256,
                Base64.getEncoder().encodeToString("another-secret-another-secret-32".getBytes()),
                Duration.ofMinutes(14), null));

        String token = other.generateAccessToken(new UserIdentity(7L, "a@pathwise.test", UserRole.USER), "s").token();

        assertThat(accessTokenManager.verifyAccessToken(token))
                .isEqualTo(new AccessTokenVerification.Rejected(JwtRejectReason.BAD_SIGNATURE));
    }

    @Test
    void verifyAccessToken_whenNotAJwt_rejectMalformed() {
        assertThat(accessTokenManager.verifyAccessToken("not-a-jwt"))
                .isEqualTo(new AccessTokenVerification.Rejected(JwtRejectReason.MALFORMED));
    }

    @Test
    void verifyAccessToken_whenExpired_rejectExpired() {
        AccessTokenManager shortLived = new AccessTokenManager(
                new HMACJwtTokenGenerator(JWSAlgorithm.HS256, SECRET, Duration.ofMillis(1), null));
        String token = shortLived.generateAccessToken(new UserIdentity(7L, "a@pathwise.test", UserRole.USER), "s").token();

        await(Duration.ofMillis(1100));

        assertThat(shortLived.verifyAccessToken(token))
                .isEqualTo(new AccessTokenVerification.Rejected(JwtRejectReason.EXPIRED));
    }

    @Test
    void generateAccessToken_whenIssued_expiresAtMatchesConfiguredLifetime() {
        Instant before = Instant.now();

        IssuedAccessToken issued = accessTokenManager.generateAccessToken(new UserIdentity(7L, "a@pathwise.test", UserRole.USER), "s");

        assertThat(issued.expiresAt())
                .isAfter(before.plus(Duration.ofMinutes(14)).minusSeconds(2))
                .isBeforeOrEqualTo(Instant.now().plus(Duration.ofMinutes(14)));
    }

    private static void await(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
