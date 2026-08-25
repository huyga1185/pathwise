package uk.huy.pathwise.routeoptimizer.infrastructure.geocoding;

import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;

public interface GeocodingProvider {
    Coordinate geocode(String address) throws AppException;
    List<Coordinate> geocodeListAddress(List<String> addresses);
}
