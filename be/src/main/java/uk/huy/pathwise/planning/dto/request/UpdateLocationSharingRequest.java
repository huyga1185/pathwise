package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.NotNull;
import uk.huy.pathwise.planning.model.LocationSharingState;

public record UpdateLocationSharingRequest(@NotNull(message = "ROUTE_ID_REQUIRED") Long routeId,
                                           @NotNull(message = "SHARING_STATE_REQUIRED") LocationSharingState state) {}
