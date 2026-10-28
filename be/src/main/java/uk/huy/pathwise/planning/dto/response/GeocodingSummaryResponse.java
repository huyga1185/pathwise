package uk.huy.pathwise.planning.dto.response;

/**
 * Stops of a plan by {@code geocodeStatus}.
 */
public record GeocodingSummaryResponse(int total, int resolved, int provided, int pending, int failed) {}
