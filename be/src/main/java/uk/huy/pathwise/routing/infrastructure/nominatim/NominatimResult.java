package uk.huy.pathwise.routing.infrastructure.nominatim;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One item of Nominatim {@code /search?format=json}; coordinates come as strings.
 */
public record NominatimResult(@JsonProperty("display_name") String displayName, String lat, String lon) {}
