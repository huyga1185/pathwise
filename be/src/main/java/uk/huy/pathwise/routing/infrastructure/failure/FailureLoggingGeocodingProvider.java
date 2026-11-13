package uk.huy.pathwise.routing.infrastructure.failure;

import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;

import java.util.List;

/**
 * Reports failures of the wrapped geocoding provider (T54). Sits under the cache: a cache hit calls no server.
 * The query (an address) is never reported.
 */
public class FailureLoggingGeocodingProvider implements GeocodingProvider {
    private final GeocodingProvider delegate;
    private final String operation;
    private final FailureLog failureLog;

    /**
     * @param providerName e.g. {@code nominatim}: the operation is reported as {@code nominatim.search}
     */
    public FailureLoggingGeocodingProvider(GeocodingProvider delegate, String providerName, FailureLog failureLog) {
        this.delegate = delegate;
        this.operation = providerName + ".search";
        this.failureLog = failureLog;
    }

    @Override
    public List<GeocodingResult> search(String query, int limit) {
        return ProviderFailures.observe(failureLog, FailureSource.GEOCODING, operation, () -> delegate.search(query, limit));
    }
}
