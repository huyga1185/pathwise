package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.planning.model.LocationSharingState;

import java.time.Instant;

/**
 * Where the driver of one route of a plan is (API_SPEC §7, P-53).
 *
 * @param location the driver's last position of the last 10 minutes; null if none
 * @param sharing  the sharing state the driver's app last reported for this route; null if none
 */
public record DriverLocationResponse(long routeId,
                                     long driverId,
                                     @Nullable DriverPosition location,
                                     @Nullable DriverSharing sharing) {
    public record DriverPosition(double lat,
                                 double lon,
                                 @Nullable Double accuracyMeters,
                                 @Nullable Double headingDegrees,
                                 @Nullable Double speedMps,
                                 Instant recordedAt) {}

    public record DriverSharing(LocationSharingState state, Instant updatedAt) {}
}
