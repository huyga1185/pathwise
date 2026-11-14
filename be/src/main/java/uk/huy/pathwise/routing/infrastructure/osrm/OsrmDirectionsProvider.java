package uk.huy.pathwise.routing.infrastructure.osrm;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.directions.Directions;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.directions.DirectionsStep;
import uk.huy.pathwise.routing.directions.Maneuver;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.ArrayList;
import java.util.List;

/**
 * OSRM {@code /route} with {@code steps=true}, {@code overview=full} and {@code geometries=polyline6}: the geometry is
 * already in the format of the contract (P-57). OSRM's maneuver {@code type} and {@code modifier} are mapped onto
 * {@link Maneuver}; distances and durations are rounded to whole metres and seconds. Any answer without exactly one
 * route of one leg with steps, including {@code NoRoute}, is a provider error.
 */
public class OsrmDirectionsProvider implements DirectionsProvider {
    private final RestClient restClient;
    private final String profile;

    public OsrmDirectionsProvider(RestClient restClient, String profile) {
        this.restClient = restClient;
        this.profile = profile;
    }

    @Override
    public Directions directions(Coordinate from, Coordinate to) {
        if (from == null || to == null) throw new IllegalArgumentException("Both points are required");
        String coordinates = from.lon() + "," + from.lat() + ";" + to.lon() + "," + to.lat();
        OsrmRouteResult result;
        try {
            result = restClient.get()
                    .uri(uriBuilder -> uriBuilder.pathSegment("route", "v1", profile, coordinates)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "polyline6")
                            .queryParam("steps", "true")
                            .build())
                    .retrieve()
                    .body(OsrmRouteResult.class);
        } catch (RestClientException e) {
            // HTTP error status (OSRM answers 400 with a code such as "NoRoute"), timeout, connection or parse failure.
            throw error("OSRM route request failed", e);
        }
        if (result == null || !"Ok".equals(result.code()))
            throw error("OSRM route returned code " + (result == null ? null : result.code()));
        if (result.routes() == null || result.routes().isEmpty()) throw error("OSRM route returned no route");
        OsrmRouteResult.Route route = result.routes().getFirst();
        if (route.geometry() == null || route.geometry().isEmpty()) throw error("OSRM route has no geometry");
        if (route.legs() == null || route.legs().size() != 1) throw error("OSRM route does not have exactly one leg");
        List<OsrmRouteResult.Step> osrmSteps = route.legs().getFirst().steps();
        if (osrmSteps == null || osrmSteps.isEmpty()) throw error("OSRM route has no steps");

        List<DirectionsStep> steps = new ArrayList<>(osrmSteps.size());
        for (OsrmRouteResult.Step step : osrmSteps) steps.add(toStep(step));
        return new Directions(round(route.distance(), "distance"), round(route.duration(), "duration"),
                route.geometry(), List.copyOf(steps));
    }

    private static DirectionsStep toStep(OsrmRouteResult.Step step) {
        OsrmRouteResult.Maneuver maneuver = step.maneuver();
        if (maneuver == null || maneuver.type() == null) throw error("OSRM step has no maneuver");
        if (maneuver.location() == null || maneuver.location().size() != 2
                || maneuver.location().get(0) == null || maneuver.location().get(1) == null)
            throw error("OSRM step has no maneuver location");
        Maneuver type = maneuver(maneuver.type(), maneuver.modifier());
        Integer exit = type == Maneuver.ROUNDABOUT && maneuver.exit() != null && maneuver.exit() > 0 ? maneuver.exit() : null;
        String name = type == Maneuver.ARRIVE || step.name() == null || step.name().isBlank() ? null : step.name().strip();
        return new DirectionsStep(type, exit, name,
                round(step.distance(), "step distance"), round(step.duration(), "step duration"),
                new Coordinate(maneuver.location().get(1), maneuver.location().get(0)));
    }

    /**
     * OSRM types (https://project-osrm.org/docs/v5.24.0/api/#stepmaneuver-object): {@code depart}, {@code arrive},
     * roundabouts and rotaries become their own maneuvers; branches ({@code fork}, {@code merge}, ramps) become
     * {@code KEEP_*} unless straight; every other type ({@code turn}, {@code new name}, {@code continue},
     * {@code end of road}, {@code roundabout turn}, {@code notification}, future ones) follows its modifier.
     */
    static Maneuver maneuver(String type, String modifier) {
        return switch (type) {
            case "depart" -> Maneuver.DEPART;
            case "arrive" -> Maneuver.ARRIVE;
            case "roundabout", "rotary", "exit roundabout", "exit rotary" -> Maneuver.ROUNDABOUT;
            case "fork", "merge", "on ramp", "off ramp" -> branch(modifier);
            default -> direction(modifier);
        };
    }

    private static Maneuver branch(String modifier) {
        if (modifier == null) return Maneuver.STRAIGHT;
        if (modifier.contains("left")) return Maneuver.KEEP_LEFT;
        if (modifier.contains("right")) return Maneuver.KEEP_RIGHT;
        return Maneuver.STRAIGHT;
    }

    private static Maneuver direction(String modifier) {
        if (modifier == null) return Maneuver.STRAIGHT;
        return switch (modifier) {
            case "uturn" -> Maneuver.UTURN;
            case "sharp right" -> Maneuver.SHARP_RIGHT;
            case "right" -> Maneuver.RIGHT;
            case "slight right" -> Maneuver.SLIGHT_RIGHT;
            case "slight left" -> Maneuver.SLIGHT_LEFT;
            case "left" -> Maneuver.LEFT;
            case "sharp left" -> Maneuver.SHARP_LEFT;
            default -> Maneuver.STRAIGHT;
        };
    }

    private static int round(Double value, String field) {
        if (value == null || value < 0 || value > Integer.MAX_VALUE) throw error("OSRM route has an invalid " + field);
        return (int) Math.round(value);
    }

    private static ExternalServiceException error(String message) {
        return error(message, null);
    }

    private static ExternalServiceException error(String message, Throwable cause) {
        return new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR, message, cause);
    }
}
