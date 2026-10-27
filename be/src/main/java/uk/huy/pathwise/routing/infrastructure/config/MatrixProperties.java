package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code routing.matrix.*} (ARCHITECTURE.md §8.5). Environment variables follow Spring's relaxed binding, e.g.
 * {@code ROUTING_MATRIX_OSRM_BASE_URL}.
 *
 * @param provider implementation to use; only {@code osrm} for now (Google later, I-03)
 */
@ConfigurationProperties(prefix = "routing.matrix")
public record MatrixProperties(@DefaultValue("osrm") String provider,
                               @DefaultValue Osrm osrm) {
    /**
     * @param profile      OSRM routing profile in the URL path; the public demo server and our self-hosted one use
     *                     {@code driving}
     * @param maxTableSize the server's {@code --max-table-size}; larger matrices are fetched in several requests
     */
    public record Osrm(@DefaultValue("http://router.project-osrm.org") String baseUrl,
                       @DefaultValue("PathWise/1.0 (https://huy-vu.uk/)") String userAgent,
                       @DefaultValue("driving") String profile,
                       @DefaultValue("100") int maxTableSize,
                       @DefaultValue("3s") Duration connectTimeout,
                       @DefaultValue("10s") Duration readTimeout) {}
}
