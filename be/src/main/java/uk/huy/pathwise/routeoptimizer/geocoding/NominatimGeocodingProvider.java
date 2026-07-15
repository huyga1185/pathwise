package uk.huy.pathwise.routeoptimizer.geocoding;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

@Component
@Qualifier("NominatimGeocodingProvider")
public class NominatimGeocodingProvider implements GeocodingProvider{
    private final RestClient restClient;

    public NominatimGeocodingProvider(@Qualifier("nominatimRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Coordinate geocode(String address) throws AppException {
        List<NominatimResult> result = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("q", address)
                        .queryParam("format", "json")
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<NominatimResult>>() {});
        if (result == null || result.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_FIND_ADDRESS);
        NominatimResult nominatimResult = result.getFirst();
        return new Coordinate(Double.parseDouble(nominatimResult.lat()), Double.parseDouble(nominatimResult.lon()));
    }

    @Override
    public List<Coordinate> geocodeListAddress(List<String> addresses) {
//        return addresses.stream().map(this::geocode).toList();
        List<Coordinate> rs = new ArrayList<>();
        for (String address : addresses) {
            rs.add(geocode(address));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AppException(ErrorCode.SERVER_ERROR);
            }
        }
        return rs;
    }
}
