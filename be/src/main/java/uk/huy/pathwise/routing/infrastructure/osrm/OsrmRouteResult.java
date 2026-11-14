package uk.huy.pathwise.routing.infrastructure.osrm;

import java.util.List;

/**
 * The fields PathWise reads from an OSRM {@code /route} response with {@code steps=true} and
 * {@code geometries=polyline6}.
 */
public record OsrmRouteResult(String code, List<Route> routes) {
    public record Route(Double distance, Double duration, String geometry, List<Leg> legs) {}

    public record Leg(List<Step> steps) {}

    public record Step(Double distance, Double duration, String name, Maneuver maneuver) {}

    /**
     * @param location [lon, lat]
     * @param exit     the exit to take, for roundabouts
     */
    public record Maneuver(String type, String modifier, List<Double> location, Integer exit) {}
}
