package uk.huy.pathwise.auth.token.jwt.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.GeneratedOtpToken;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtClaimsSet;
import uk.huy.pathwise.auth.token.jwt.infrastructure.exception.JwtException;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class OtpTokenService {
    private final JwtTokenGenerator jwtTokenGenerator;

    public OtpTokenService(@Qualifier("hmacOtpTokenGenerator") JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    public GeneratedOtpToken generateOtpToken(UserIdentity identity, String purpose) {
        if (identity == null) throw new NullPointerException("UserIdentity could not be null");
        if (purpose == null) throw new NullPointerException("Purpose could not be null");
        if (purpose.isEmpty()) throw new IllegalArgumentException("Purpose could not be empty");
        return new GeneratedOtpToken(jwtTokenGenerator.generateJwt(String.valueOf(identity.id()),
                null,
                null,
                null,
                Map.of("role", identity.role(),
                        "purpose", purpose)), Instant.now().plus(jwtTokenGenerator.getExpirationTime()));
    }

    public Optional<String> verifyOtpToken(UserIdentity identity, String otpToken) {
        Optional<JwtClaimsSet> optionalJWTClaimsSet = jwtTokenGenerator.verifyJwt(otpToken, null);
        if (optionalJWTClaimsSet.isEmpty()) return Optional.empty();
        JwtClaimsSet jwtClaimsSet = optionalJWTClaimsSet.get();
        if (jwtClaimsSet.sub() == null ||
                jwtClaimsSet.sub().isEmpty() ||
                Long.parseLong(jwtClaimsSet.sub()) != identity.id()) {
            log.warn("Bad otp token identity id");
            return Optional.empty();
        }
        try {
            UserRole role = UserRole.valueOf(jwtClaimsSet.getClaimAsString("role"));
            if (!role.equals(identity.role())) return Optional.empty();
        } catch (JwtException ignored) {
            log.warn("Bad otp token identity role");
            return Optional.empty();
        }
        String purpose;
        try {
            purpose = jwtClaimsSet.getClaimAsString("purpose");
        } catch (JwtException ignored) {
            log.warn("Bad otp token purpose");
            return Optional.empty();
        }

        if (purpose == null) {
            log.warn("Bad otp token purpose");
            return Optional.empty();
        }

        return Optional.of(purpose);
    }
}
