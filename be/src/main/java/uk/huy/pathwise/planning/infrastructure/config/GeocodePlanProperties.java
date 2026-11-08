package uk.huy.pathwise.planning.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code jobs.geocode-plan.*} read by the geocoding job; the mode itself is read by the {@code job} module.
 *
 * @param batchSize stops written per transaction, so that polling clients see progress (ARCHITECTURE.md §8.2)
 */
@ConfigurationProperties("jobs.geocode-plan")
public record GeocodePlanProperties(@DefaultValue("5") int batchSize) {
    public GeocodePlanProperties {
        if (batchSize <= 0) throw new IllegalArgumentException("jobs.geocode-plan.batch-size must be positive");
    }
}
