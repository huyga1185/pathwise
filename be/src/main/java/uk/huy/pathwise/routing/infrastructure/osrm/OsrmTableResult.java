package uk.huy.pathwise.routing.infrastructure.osrm;

/**
 * OSRM {@code /table} response. A cell is null when OSRM found no route between the two points.
 */
public record OsrmTableResult(String code, Double[][] distances, Double[][] durations) {}
