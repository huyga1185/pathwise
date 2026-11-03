package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.RouteStatus;

/**
 * Response of a stop status update by the driver (API_SPEC §8, P-32): the app learns whether its route and the plan
 * have moved to IN_PROGRESS or COMPLETED (BR-11).
 *
 * @param routeStatus status of the caller's route, as {@code status} of {@code DriverRouteDetail} (P-35)
 */
public record DriverStopUpdateResponse(DriverStopResponse stop, RouteStatus routeStatus, PlanStatus planStatus) {}
