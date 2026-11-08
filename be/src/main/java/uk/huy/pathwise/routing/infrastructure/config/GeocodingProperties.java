package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code routing.geocoding.*} (ARCHITECTURE.md §8.5, D-25). Environment variables follow Spring's relaxed binding,
 * e.g. {@code ROUTING_GEOCODING_NOMINATIM_BASE_URL}, {@code ROUTING_GEOCODING_NOMINATIM_RATE_LIMIT_ENABLED}.
 *
 * @param provider implementation to use; only {@code nominatim} for now (Google later, I-03)
 * @param cacheTtl how long a search result stays cached (P-11)
 */
@ConfigurationProperties(prefix = "routing.geocoding")
public record GeocodingProperties(@DefaultValue("nominatim") String provider,
                                  @DefaultValue("24h") Duration cacheTtl,
                                  @DefaultValue Nominatim nominatim) {
    /**
     * @param userAgent required by the Nominatim usage policy
     */
    public record Nominatim(@DefaultValue("https://nominatim.openstreetmap.org") String baseUrl,
                            @DefaultValue("PathWise/1.0 (https://huy-vu.uk/)") String userAgent,
                            @DefaultValue("3s") Duration connectTimeout,
                            @DefaultValue("10s") Duration readTimeout,
                            @DefaultValue RateLimit rateLimit) {}

    /**
     * @param enabled   false for a self-hosted Nominatim (D-25)
     * @param perSecond requests per second across all instances
     * @param maxWait   how long a request waits for a slot before answering 503 {@code GEOCODING_BUSY}
     */
    public record RateLimit(@DefaultValue("true") boolean enabled,
                            @DefaultValue("1") int perSecond,
                            @DefaultValue("2s") Duration maxWait) {}
}
