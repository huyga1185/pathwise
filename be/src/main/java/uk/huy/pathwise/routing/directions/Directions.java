package uk.huy.pathwise.routing.directions;

import java.util.List;

/**
 * A driving route between two points (FR-D05, P-57).
 *
 * @param polyline the path, in the Encoded Polyline Algorithm Format with a precision of 6 decimals, [lat, lon]
 * @param steps    the instructions in order: the first is {@link Maneuver#DEPART}, the last {@link Maneuver#ARRIVE}
 */
public record Directions(int distanceMeters, int durationSeconds, String polyline, List<DirectionsStep> steps) {}
