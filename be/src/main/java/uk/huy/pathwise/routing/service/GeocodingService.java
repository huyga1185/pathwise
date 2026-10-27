package uk.huy.pathwise.routing.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.routing.dto.response.PlaceResponse;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;

import java.util.List;

/**
 * Address search for autocomplete and for fixing stops that failed to geocode (FR-P01). Shares the cache and the rate
 * limit with background geocoding (P-11).
 */
@Service
@RequiredArgsConstructor
public class GeocodingService {
    private final GeocodingProvider geocodingProvider;

    public List<PlaceResponse> search(String query, int limit) {
        return geocodingProvider.search(query, limit).stream()
                .map(result -> new PlaceResponse(result.displayName(), result.lat(), result.lon()))
                .toList();
    }
}
