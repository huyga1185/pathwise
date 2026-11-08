package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.planning.model.GeocodeError;
import uk.huy.pathwise.planning.model.GeocodeStatus;
import uk.huy.pathwise.planning.model.StopStatus;

import java.time.Instant;
import java.util.Map;

/**
 * A stop in {@code PlanDetail}, the same shape inside a route and in {@code unassignedStops}.
 *
 * @param recipientPhone E.164
 * @param demands        only the units with a non-zero demand
 * @param timeWindow     null for no window
 * @param dropReason     why the stop has no route; null inside a route
 */
public record PlanStopResponse(long id,
                               @Nullable Integer sequence,
                               String label,
                               @Nullable String recipientName,
                               @Nullable String recipientPhone,
                               @Nullable String address,
                               @Nullable Double lat,
                               @Nullable Double lon,
                               GeocodeStatus geocodeStatus,
                               @Nullable GeocodeError geocodeError,
                               @Nullable String geocodedDisplayName,
                               Map<CapacityUnit, Integer> demands,
                               int serviceTimeSeconds,
                               @Nullable TimeWindowResponse timeWindow,
                               @Nullable String note,
                               @Nullable Instant plannedArrival,
                               StopStatus status,
                               @Nullable String failureReason,
                               @Nullable DropReason dropReason) {}
