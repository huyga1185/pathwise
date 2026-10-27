package uk.huy.pathwise.routing.infrastructure.nominatim;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;
import uk.huy.pathwise.routing.infrastructure.throttle.RequestThrottle;

import java.util.List;

/**
 * Nominatim {@code /search}, restricted to Vietnam. Every call takes a slot of the shared throttle first (Nominatim
 * usage policy: at most 1 request per second, P-11); the throttle is a no-op when disabled for a self-hosted
 * Nominatim (D-25).
 */
public class NominatimGeocodingProvider implements GeocodingProvider {
    private static final ParameterizedTypeReference<List<NominatimResult>> RESULTS = new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final RequestThrottle throttle;

    public NominatimGeocodingProvider(RestClient restClient, RequestThrottle throttle) {
        this.restClient = restClient;
        this.throttle = throttle;
    }

    @Override
    public List<GeocodingResult> search(String query, int limit) {
        if (!throttle.acquire())
            throw new ExternalServiceException(ErrorCode.GEOCODING_BUSY, "No Nominatim rate limit slot in time", null);
        List<NominatimResult> results;
        try {
            results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("countrycodes", "vn")
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(RESULTS);
        } catch (RestClientException e) {
            // HTTP error status, timeout, connection failure or unreadable body.
            throw new ExternalServiceException(ErrorCode.GEOCODING_PROVIDER_ERROR, "Nominatim search failed", e);
        }
        if (results == null) return List.of();
        try {
            return results.stream()
                    .map(result -> new GeocodingResult(result.displayName(),
                            Double.parseDouble(result.lat()),
                            Double.parseDouble(result.lon())))
                    .toList();
        } catch (NumberFormatException | NullPointerException e) {
            throw new ExternalServiceException(ErrorCode.GEOCODING_PROVIDER_ERROR, "Nominatim returned invalid coordinates", e);
        }
    }
}
