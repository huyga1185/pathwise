package uk.huy.pathwise.auth.infastructure.token.jwt;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
@Slf4j
public class JwtService {
    private final JWSSigner signer;
    private final JWSVerifier verifier;
    private final Duration expirationTime;

    public JwtService(@Value("${jwt.secret}") String secretKey,
                      @Value("${jwt.expirationTime:15m}") Duration expirationTime) throws JOSEException {
        byte[] secret = Base64.getDecoder().decode(secretKey);
        signer = new MACSigner(secret);
        verifier = new MACVerifier(secret);
        this.expirationTime = expirationTime;
    }

    public String generateJwt(UserIdentity identity) throws JOSEException {
        if (identity == null) throw new NullPointerException("Identity is Null");
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        Instant now = Instant.now();

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(String.valueOf(identity.id()))
                .claim("role", identity.role().name())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(expirationTime)))
                .build();

        SignedJWT signedJWT = new SignedJWT(jwsHeader, jwtClaimsSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public Optional<UserIdentity> verifyJwt(String jwt) {
        if (jwt == null) throw new NullPointerException("Jwt is null");
        try {
            SignedJWT signedJWT = SignedJWT.parse(jwt);
            if (!signedJWT.verify(verifier)) {
                log.warn("Bad signature");
                return Optional.empty();
            }
            JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            if (Date.from(Instant.now()).after(jwtClaimsSet.getExpirationTime()))
                return Optional.empty();
            return Optional.of(new UserIdentity(Long.parseLong(jwtClaimsSet.getSubject()),
                    UserRole.valueOf((String) jwtClaimsSet.getClaim("role"))));
        } catch (ParseException ignored) {
            return Optional.empty();
        } catch (JOSEException e) {
            log.warn("Verifier could not work", e);
            return Optional.empty();
        }
    }
}
