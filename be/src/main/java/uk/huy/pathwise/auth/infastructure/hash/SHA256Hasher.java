package uk.huy.pathwise.auth.infastructure.hash;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class SHA256Hasher implements Hasher {
    @Override
    public String hashString(String rawString) {
        if (rawString == null) throw new NullPointerException("Raw string could not be null");
        if (rawString.isEmpty()) throw new IllegalArgumentException("Raw string could not be empty");
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 is not available", e);
        }
        return HexFormat.of().formatHex(messageDigest.digest(rawString.getBytes(StandardCharsets.UTF_8)));
    }
}
