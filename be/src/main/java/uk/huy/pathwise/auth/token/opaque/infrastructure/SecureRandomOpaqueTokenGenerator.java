package uk.huy.pathwise.auth.token.opaque.infrastructure;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SecureRandomOpaqueTokenGenerator implements OpaqueTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
