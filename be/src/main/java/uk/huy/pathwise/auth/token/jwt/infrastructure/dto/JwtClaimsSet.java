package uk.huy.pathwise.auth.token.jwt.infrastructure.dto;

import uk.huy.pathwise.auth.token.jwt.infrastructure.exception.JwtException;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @param sub    Subject
 * @param aud    Audiences
 * @param jti    Jwt token id
 * @param nbf    Not before time
 * @param iss    Issue time
 * @param exp    Expiration time
 * @param claims Claims
 */
public record JwtClaimsSet(String sub,
                           List<String> aud,
                           String jti,
                           Date nbf,
                           Date iss,
                           Date exp,
                           Map<String, Object> claims) {
    public Object getClaim(String key) {
        return claims.get(key);
    }

    public String getClaimAsString(String key) {
        Object value = claims.get(key);

        if (value == null) return null;

        if (!(value instanceof String)) throw new JwtException("Claim is not String: " + key);

        return (String) value;
    }
}
