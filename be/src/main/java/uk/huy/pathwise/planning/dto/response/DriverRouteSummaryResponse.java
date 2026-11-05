package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.planning.model.RouteStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One route in {@code GET /driver/routes} (API_SPEC §8).
 *
 * @param timezone       IANA zone id of the plan (the store's when the plan was saved, P-14), to show
 *                       {@code plannedStart} as the store's local time (T47)
 * @param status         of this route, from its own stops
 * @param completedCount stops that have finished: DELIVERED, FAILED or SKIPPED
 */
public record DriverRouteSummaryResponse(long routeId,
                                         long planId,
                                         String planName,
                                         LocalDate planDate,
                                         String timezone,
                                         RouteStatus status,
                                         int stopCount,
                                         int completedCount,
                                         @Nullable Instant plannedStart) {}
