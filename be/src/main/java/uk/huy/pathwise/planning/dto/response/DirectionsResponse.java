package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.routing.directions.Maneuver;

import java.util.List;

/**
 * Driving directions from the driver's position to one point of the route (API_SPEC §8, T33, P-57).
 *
 * @param polyline the path, Encoded Polyline Algorithm Format with a precision of 6 decimals, [lat, lon]
 * @param steps    the instructions in order: the first is {@code DEPART}, the last {@code ARRIVE}
 */
public record DirectionsResponse(DirectionsDestination destination,
                                 int distanceMeters,
                                 int durationSeconds,
                                 String polyline,
                                 List<DirectionsInstruction> steps) {
    /**
     * @param stopId the stop driven to; null for the end of the route
     */
    public record DirectionsDestination(@Nullable Long stopId, double lat, double lon) {}

    /**
     * One step; not named {@code Step}, which Flutter's Material library also defines (generated Dart client). The
     * maneuver happens at {@code location}; the driver then travels {@code distanceMeters} in {@code durationSeconds}
     * until the next step.
     *
     * @param exitNumber which exit of a {@code ROUNDABOUT} to take, from 1; null otherwise or if unknown
     * @param roadName   the road after the maneuver; null if unnamed, and for {@code ARRIVE}
     */
    public record DirectionsInstruction(Maneuver maneuver,
                                        @Nullable Integer exitNumber,
                                        @Nullable String roadName,
                                        int distanceMeters,
                                        int durationSeconds,
                                        DirectionsPoint location) {}

    public record DirectionsPoint(double lat, double lon) {}
}
