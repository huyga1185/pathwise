package uk.huy.pathwise.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * {@code cors.*} (ARCHITECTURE.md §6.4): web origins allowed to call the API from a browser, with credentials.
 *
 * @param allowedOrigins exact origins such as {@code https://app.example.com}, env {@code CORS_ALLOWED_ORIGINS}
 *                       (comma separated); none by default, so no other origin is allowed. Mobile apps send no
 *                       {@code Origin} and are not affected.
 */
@ConfigurationProperties("cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
