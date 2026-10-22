package uk.huy.pathwise.auth.infrastructure.builder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class AuthCookieBuilder {
    private final String sameSite;

    /**
     * @param sameSite SameSite attribute of every auth cookie (D-22, ARCHITECTURE.md §6.4), from {@code auth.cookie.same-site}.
     */
    public AuthCookieBuilder(@Value("${auth.cookie.same-site:Strict}") String sameSite) {
        this.sameSite = sameSite;
    }

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
                .sameSite(sameSite)
                .maxAge(Duration.between(Instant.now(), maxAge))
                .build();
    }

    public ResponseCookie removeSecureCookie(String name, String value, String path) {
        this.validateArgs(name, value, false);
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path(path)
                .sameSite(sameSite)
                .maxAge(0)
                .build();
    }
}
