package uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.hmacgenerator;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;
import uk.huy.pathwise.auth.token.jwt.infrastructure.exception.JwtException;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.AbstractNimbusJwtTokenGenerator;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
public class HMACJwtTokenGenerator extends AbstractNimbusJwtTokenGenerator {

    public HMACJwtTokenGenerator(JWSAlgorithm jwsAlgorithm,
                                    String secretKey,
                                    Duration expirationTime,
                                    String issuer) {
        super(Set.of(JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512),
                jwsAlgorithm,
                secretKey,
                expirationTime, issuer);
    }

    @Override
    public final String generateJwt(String subject,
                                       List<String> audience,
                                       String jti,
                                       Duration nbf,
                                       Map<String, Object> claims) {
        if (subject == null) throw new NullPointerException("Subject could not be null");
        if (subject.isEmpty()) throw new IllegalArgumentException("Subject could not be empty");
        if (nbf != null && nbf.compareTo(expirationTime) >= 0) throw new IllegalStateException("Not before must shorter than expiration time");
        JWSSigner signer;
        try {
            signer = new MACSigner(secretKey);
        } catch (KeyLengthException e) {
            throw new JwtException("Secret key length is shorter than the minimum 256-bit requirement", e);
        }
        Instant now = Instant.now();
        JWSHeader jwsHeader = new JWSHeader(jwsAlgorithm);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(this.issuer)
                .subject(subject)
                .audience(audience)
                .jwtID(jti)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(this.expirationTime)));

        if (nbf != null) {
            if (nbf.isNegative()) {
                throw new IllegalArgumentException("Not before duration cannot be negative");
            }
            if (nbf.compareTo(expirationTime) >= 0) {
                throw new IllegalStateException("Not before must be shorter than expiration time");
            }
            builder.notBeforeTime(Date.from(now.plus(nbf)));
        }

        if (claims != null) {
            claims.forEach((key, value) -> {
                if (reserved.contains(key)) throw new IllegalArgumentException("Reserved claim: " + key);
                builder.claim(key, value);
            });
        }

        JWTClaimsSet jwtClaimsSet = builder.build();
        SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaimsSet);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new JwtException("Could not sign jwt", e);
        }
        return signedJWT.serialize();
    }

    public final Optional<JwtClaimsSet> verifyJwt(String jwt, String expectedAudience) {
        if (jwt == null) throw new NullPointerException("Jwt could not be null");
        if (jwt.isEmpty()) throw new IllegalArgumentException("Jwt could not be empty");
        Instant now = Instant.now();
        Date nowDate = Date.from(now);
        try {
            MACVerifier verifier = new MACVerifier(secretKey);
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            if (!signedJWT.verify(verifier)) {
                log.warn("Bad signature");
                return Optional.empty();
            }
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            if (!jwsAlgorithm.equals(signedJWT.getHeader().getAlgorithm()))
                return Optional.empty();
            if (issuer != null && !Objects.equals(this.issuer, jwtClaimsSet.getIssuer()))
                return Optional.empty();
            if (jwtClaimsSet.getAudience() != null &&
                    expectedAudience != null &&
                    !jwtClaimsSet.getAudience().contains(expectedAudience))
                return Optional.empty();
            if (jwtClaimsSet.getNotBeforeTime() != null && nowDate.before(jwtClaimsSet.getNotBeforeTime()))
                return Optional.empty();
            if (jwtClaimsSet.getExpirationTime() == null || nowDate.after(jwtClaimsSet.getExpirationTime()))
                return Optional.empty();
            JwtClaimsSet result = new JwtClaimsSet(
                    jwtClaimsSet.getSubject(),
                    jwtClaimsSet.getAudience(),
                    jwtClaimsSet.getJWTID(),
                    jwtClaimsSet.getNotBeforeTime(),
                    jwtClaimsSet.getIssueTime(),
                    jwtClaimsSet.getExpirationTime(),
                    jwtClaimsSet.getClaims()
            );
            return Optional.of(result);
        } catch (JOSEException e) {
            log.warn("Verifier could not work");
            return Optional.empty();
        } catch (ParseException ignored) {
            return Optional.empty();
        }
    }
}
