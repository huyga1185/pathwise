package uk.huy.pathwise.planning.dto.response;

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
                               Integer sequence,
                               String label,
                               String recipientName,
                               String recipientPhone,
                               String address,
                               Double lat,
                               Double lon,
                               GeocodeStatus geocodeStatus,
                               GeocodeError geocodeError,
                               String geocodedDisplayName,
                               Map<CapacityUnit, Integer> demands,
                               int serviceTimeSeconds,
                               TimeWindowResponse timeWindow,
                               String note,
                               Instant plannedArrival,
                               StopStatus status,
                               String failureReason,
                               DropReason dropReason) {}
