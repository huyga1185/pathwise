package uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus;

import com.nimbusds.jose.JWSAlgorithm;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;

import java.time.Duration;
import java.util.Base64;
import java.util.Set;

public abstract class AbstractNimbusJwtTokenGenerator implements JwtTokenGenerator {
    protected final byte[] secretKey;
    protected final Duration expirationTime;
    protected final String issuer;
    protected final JWSAlgorithm jwsAlgorithm;
    protected static final Set<String> reserved = Set.of("iss", "sub", "aud", "exp", "nbf", "iat", "jti");

    protected AbstractNimbusJwtTokenGenerator(Set<JWSAlgorithm> allowedAlgorithms,
                                        JWSAlgorithm jwsAlgorithm,
                                        String secretKey,
                                        Duration expirationTime,
                                        String issuer) {
        if (secretKey == null) throw new NullPointerException("Secret key could not be null");
        if (secretKey.isEmpty()) throw new IllegalArgumentException("Secret key could not be empty");
        if (expirationTime == null) throw new NullPointerException("Expiration time could not be null");
        if (expirationTime.isZero() || expirationTime.isNegative()) throw new IllegalArgumentException("Expiration time must be positive");
        if (jwsAlgorithm == null) throw new NullPointerException("JWSAlgorithm could not be null");
        this.secretKey = Base64.getDecoder().decode(secretKey);
        this.expirationTime = expirationTime;
        this.issuer = issuer;
        if (!allowedAlgorithms.contains(jwsAlgorithm)) throw new IllegalStateException(jwsAlgorithm.getName() + " is not allowed");
        this.jwsAlgorithm = jwsAlgorithm;
    }

    @Override
    public Duration getExpirationTime() {
        return this.expirationTime;
    }
}
