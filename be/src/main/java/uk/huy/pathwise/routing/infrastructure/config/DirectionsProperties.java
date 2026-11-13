package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code routing.directions.*} (ARCHITECTURE.md §8.5, P-57). Environment variables follow Spring's relaxed binding,
 * e.g. {@code ROUTING_DIRECTIONS_OSRM_BASE_URL}; by default the same OSRM server as the matrix.
 *
 * @param provider implementation to use; only {@code osrm} for now
 */
@ConfigurationProperties(prefix = "routing.directions")
public record DirectionsProperties(@DefaultValue("osrm") String provider,
                                   @DefaultValue Osrm osrm) {
    /**
     * @param profile OSRM routing profile in the URL path ({@code driving})
     */
    public record Osrm(@DefaultValue("http://router.project-osrm.org") String baseUrl,
                       @DefaultValue("PathWise/1.0 (https://huy-vu.uk/)") String userAgent,
                       @DefaultValue("driving") String profile,
                       @DefaultValue("3s") Duration connectTimeout,
                       @DefaultValue("10s") Duration readTimeout) {}
}
