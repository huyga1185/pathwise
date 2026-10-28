package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.ProcessingStatus;

import java.time.Instant;
import java.time.LocalDate;

/**
 * {@code PlanSummary} of API_SPEC §7, one row of {@code GET /plans}.
 */
public record PlanSummaryResponse(long id,
                                  String name,
                                  LocalDate planDate,
                                  PlanStatus status,
                                  ProcessingStatus processingStatus,
                                  long version,
                                  long stopCount,
                                  long routeCount,
                                  Instant optimizedAt,
                                  Integer totalDistanceMeters,
                                  Integer totalDurationSeconds,
                                  Instant createdAt) {}
