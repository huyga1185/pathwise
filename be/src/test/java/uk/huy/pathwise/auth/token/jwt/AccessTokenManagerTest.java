package uk.huy.pathwise.auth.token.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.Test;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.hmacgenerator.HMACJwtTokenGenerator;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenManagerTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[32]);

    private final AccessTokenManager accessTokenManager = new AccessTokenManager(
            new HMACJwtTokenGenerator(JWSAlgorithm.HS256, SECRET, Duration.ofMinutes(14), null));

    @Test
    void verifyAccessToken_whenTokenGenerated_returnSameIdentity() {
        UserIdentity identity = new UserIdentity(7L, "admin@pathwise.test", UserRole.ADMIN);

        String token = accessTokenManager.generateAccessToken(identity).token();
        Optional<UserIdentity> verified = accessTokenManager.verifyAccessToken(token);

        assertThat(verified).contains(identity);
    }

    @Test
    void verifyAccessToken_whenTokenSignedWithAnotherSecret_returnEmpty() {
        AccessTokenManager other = new AccessTokenManager(new HMACJwtTokenGenerator(JWSAlgorithm.HS256,
                Base64.getEncoder().encodeToString("another-secret-another-secret-32".getBytes()),
                Duration.ofMinutes(14), null));

        String token = other.generateAccessToken(new UserIdentity(7L, "a@pathwise.test", UserRole.USER)).token();

        assertThat(accessTokenManager.verifyAccessToken(token)).isEmpty();
    }

    @Test
    void generateAccessToken_whenIssued_expiresAtMatchesConfiguredLifetime() {
        Instant before = Instant.now();

        IssuedAccessToken issued = accessTokenManager.generateAccessToken(new UserIdentity(7L, "a@pathwise.test", UserRole.USER));

        assertThat(issued.expiresAt())
                .isAfter(before.plus(Duration.ofMinutes(14)).minusSeconds(2))
                .isBeforeOrEqualTo(Instant.now().plus(Duration.ofMinutes(14)));
    }
}
