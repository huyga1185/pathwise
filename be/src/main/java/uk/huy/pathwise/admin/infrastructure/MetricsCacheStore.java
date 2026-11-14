package uk.huy.pathwise.admin.infrastructure;

import java.time.Duration;
import java.util.Optional;

/**
 * Short cache of the business metrics (T60): the same range asked again within a minute is not recomputed.
 */
public interface MetricsCacheStore {
    Optional<String> find(String key);

    void save(String key, String json, Duration ttl);
}
