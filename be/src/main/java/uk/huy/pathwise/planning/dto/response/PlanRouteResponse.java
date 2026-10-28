package uk.huy.pathwise.planning.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * @param load  total demand of the route's stops in the vehicle's capacity unit (D-36); null until optimized
 * @param stops in visiting order; empty until optimized
 */
public record PlanRouteResponse(long id,
                                Driver driver,
                                Vehicle vehicle,
                                @JsonFormat(pattern = "HH:mm") LocalTime shiftStart,
                                @JsonFormat(pattern = "HH:mm") LocalTime shiftEnd,
                                Integer maxDistanceMeters,
                                LocationResponse start,
                                LocationResponse end,
                                Integer plannedDistanceMeters,
                                Integer plannedDurationSeconds,
                                Instant plannedStart,
                                Instant plannedEnd,
                                Integer load,
                                List<PlanStopResponse> stops) {
    /**
     * Current name and role of the member, even if disabled since the assignment.
     */
    public record Driver(long id, String fullName, UserRole role) {}

    /**
     * Current name of the vehicle; {@code capacity} and {@code capacityUnit} are the snapshot of the assignment.
     */
    public record Vehicle(long id, String name, int capacity, CapacityUnit capacityUnit) {}
}
