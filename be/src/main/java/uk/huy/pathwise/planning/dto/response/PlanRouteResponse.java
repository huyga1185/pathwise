package uk.huy.pathwise.planning.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.planning.model.RouteStatus;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

/**
 * @param status the route's own status, from its stops (P-35, P-39); PENDING until the first stop is reported, so
 *               always PENDING in a draft
 * @param load   total demand of the route's stops in the vehicle's capacity unit (D-36); null until optimized
 * @param stops  in visiting order; empty until optimized
 */
public record PlanRouteResponse(long id,
                                Driver driver,
                                Vehicle vehicle,
                                RouteStatus status,
                                @JsonFormat(pattern = "HH:mm") LocalTime shiftStart,
                                @JsonFormat(pattern = "HH:mm") LocalTime shiftEnd,
                                @Nullable Integer maxDistanceMeters,
                                LocationResponse start,
                                LocationResponse end,
                                @Nullable Integer plannedDistanceMeters,
                                @Nullable Integer plannedDurationSeconds,
                                @Nullable Instant plannedStart,
                                @Nullable Instant plannedEnd,
                                @Nullable Integer load,
                                List<PlanStopResponse> stops) {
    /**
     * Current name and role of the member, even if disabled since the assignment.
     */
    public record Driver(long id, @Nullable String fullName, UserRole role) {}

    /**
     * Current name of the vehicle; {@code capacity} and {@code capacityUnit} are the snapshot of the assignment.
     */
    public record Vehicle(long id, String name, int capacity, CapacityUnit capacityUnit) {}
}
