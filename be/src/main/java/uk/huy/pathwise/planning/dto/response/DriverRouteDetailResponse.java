package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.RouteStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * {@code DriverRouteDetail} of API_SPEC §8.
 *
 * @param start the route's own start, the plan depot in the MVP (D-20)
 * @param stops in visiting order
 */
public record DriverRouteDetailResponse(long routeId,
                                        long planId,
                                        LocalDate planDate,
                                        RouteStatus status,
                                        LocationResponse start,
                                        LocationResponse end,
                                        List<DriverStopResponse> stops) {}
