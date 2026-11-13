package uk.huy.pathwise.routing.directions;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.routing.model.Coordinate;

/**
 * One instruction of a {@link Directions}: the maneuver happens at {@code location}, then the driver travels
 * {@code distanceMeters} in {@code durationSeconds} until the next step.
 *
 * @param exitNumber which exit of a {@link Maneuver#ROUNDABOUT} to take, from 1; null for other maneuvers or if unknown
 * @param roadName   the road after the maneuver; null if it has no name, and for {@link Maneuver#ARRIVE}
 */
public record DirectionsStep(Maneuver maneuver,
                             @Nullable Integer exitNumber,
                             @Nullable String roadName,
                             int distanceMeters,
                             int durationSeconds,
                             Coordinate location) {}
