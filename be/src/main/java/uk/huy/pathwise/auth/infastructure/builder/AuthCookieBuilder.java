package uk.huy.pathwise.auth.infastructure.builder;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuthCookieBuilder {
    private void validateArgs(String name, String value, boolean canValueBeNullOrEmpty) {
        if (name == null) throw new NullPointerException("Name could not be null");
        if (name.isEmpty()) throw new IllegalArgumentException("Name could not be empty");
        if (canValueBeNullOrEmpty && value == null) throw new NullPointerException("Value could not be null");
        if (canValueBeNullOrEmpty && value.isEmpty()) throw new IllegalArgumentException("Value could not be empty");
    }

    public ResponseCookie createSecureCookie(String name, String value, String path, Instant maxAge) {
        this.validateArgs(name, value, true);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(Duration.between(Instant.now(), maxAge))
                .build();
    }

    public ResponseCookie removeSecureCookie(String name, String value, String path) {
        this.validateArgs(name, value, false);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(0)
                .build();
    }
}
