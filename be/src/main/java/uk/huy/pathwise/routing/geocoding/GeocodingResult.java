package uk.huy.pathwise.routing.geocoding;

/**
 * One candidate place for an address, as returned by a geocoding provider.
 */
public record GeocodingResult(String displayName, double lat, double lon) {}
