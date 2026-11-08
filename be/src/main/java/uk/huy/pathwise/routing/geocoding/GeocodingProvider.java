package uk.huy.pathwise.routing.geocoding;

import java.util.List;

/**
 * Public API of the {@code routing} module for geocoding (ARCHITECTURE.md §8.5). The bean other modules get is
 * {@link CachingGeocodingProvider} in front of the provider chosen by {@code routing.geocoding.provider}.
 */
public interface GeocodingProvider {
    /**
     * @param query free-text address
     * @param limit maximum number of candidates
     * @return the candidates, best first; empty if nothing matched
     * @throws uk.huy.pathwise.core.exception.ExternalServiceException {@code GEOCODING_PROVIDER_ERROR} if the provider
     *         fails or times out, {@code GEOCODING_BUSY} if no rate limit slot was free in time
     */
    List<GeocodingResult> search(String query, int limit);
}
