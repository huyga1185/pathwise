package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.ProcessingStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code PlanDetail} of API_SPEC §7. {@code activeJob} comes with the job framework (T29).
 *
 * @param routes          one per assignment, by id
 * @param unassignedStops stops without a route, by id
 */
public record PlanDetailResponse(long id,
                                 String name,
                                 LocalDate planDate,
                                 PlanStatus status,
                                 long version,
                                 ProcessingStatus processingStatus,
                                 GeocodingSummaryResponse geocodingSummary,
                                 LocationResponse depot,
                                 String timezone,
                                 Instant optimizedAt,
                                 Integer totalDistanceMeters,
                                 Integer totalDurationSeconds,
                                 List<PlanRouteResponse> routes,
                                 List<PlanStopResponse> unassignedStops) {}
