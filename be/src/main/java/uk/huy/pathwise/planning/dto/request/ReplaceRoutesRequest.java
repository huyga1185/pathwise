package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body of {@code PUT /plans/{id}/routes} (API_SPEC §7, T28): every route of the plan with its stops in visiting order,
 * and the stops left without a route. Unknown, repeated or missing ids are checked by the service, which knows the plan.
 */
public record ReplaceRoutesRequest(@NotNull(message = "VERSION_REQUIRED") Long version,
                                   @NotNull(message = "ROUTES_REQUIRED") List<@Valid RouteStops> routes,
                                   @NotNull(message = "UNASSIGNED_STOP_IDS_REQUIRED") List<Long> unassignedStopIds) {
    /**
     * @param stopIds in visiting order
     */
    public record RouteStops(@NotNull(message = "ROUTE_ID_REQUIRED") Long routeId,
                             @NotNull(message = "STOP_IDS_REQUIRED") List<Long> stopIds) {}
}
