package uk.huy.pathwise.routeoptimizer.geocoding;

import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;

public interface GeocodingProvider {
    Coordinate geocode(String address) throws AppException;
    List<Coordinate> geocodeListAddress(List<String> addresses);
}
