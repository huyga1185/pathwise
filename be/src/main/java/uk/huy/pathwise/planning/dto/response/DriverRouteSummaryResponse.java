package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.RouteStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * One route in {@code GET /driver/routes} (API_SPEC §8).
 *
 * @param status         of this route, from its own stops
 * @param completedCount stops that have finished: DELIVERED, FAILED or SKIPPED
 */
public record DriverRouteSummaryResponse(long routeId,
                                         long planId,
                                         String planName,
                                         LocalDate planDate,
                                         RouteStatus status,
                                         int stopCount,
                                         int completedCount,
                                         Instant plannedStart) {}
