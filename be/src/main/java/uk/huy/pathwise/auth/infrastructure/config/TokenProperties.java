package uk.huy.pathwise.auth.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * {@code token.*} (ARCHITECTURE.md §4, F-38): lifetime of refresh tokens and the access token JWT. The secret comes
 * from {@code TOKEN_JWT_ACCESS_TOKEN_SECRET} (§10); the app does not start without it.
 *
 * @param expirationTime lifetime of a refresh token
 */
@Validated
@ConfigurationProperties("token")
public record TokenProperties(@DefaultValue("14d") Duration expirationTime, @Valid @NotNull Jwt jwt) {
    /**
     * @param accessTokenSecret           Base64, at least 256 bits
     * @param accessTokenHasingAlgorithm  JWS algorithm (the key keeps its historical spelling)
     * @param accessTokenExpirationTime   lifetime of an access token
     * @param accessTokenIssuer           {@code iss} claim, none if unset
     */
    public record Jwt(@NotBlank String accessTokenSecret,
                      @DefaultValue("HS256") String accessTokenHasingAlgorithm,
                      @DefaultValue("14m") Duration accessTokenExpirationTime,
                      @Nullable String accessTokenIssuer) {
        @Override
        public String toString() {
            // Never print the secret (logs, failure analysis).
            return "Jwt[algorithm=" + accessTokenHasingAlgorithm + ", expiration=" + accessTokenExpirationTime
                    + ", issuer=" + accessTokenIssuer + "]";
        }
    }
}
