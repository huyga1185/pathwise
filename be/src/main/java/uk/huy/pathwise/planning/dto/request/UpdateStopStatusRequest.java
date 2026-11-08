package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import uk.huy.pathwise.planning.model.StopStatus;

/**
 * Body of {@code PATCH /driver/routes/{routeId}/stops/{stopId}/status} (FR-D03, BR-10).
 *
 * @param status        DELIVERED, FAILED or SKIPPED; PENDING is never a target (P-34)
 * @param failureReason required for FAILED, ignored otherwise
 */
public record UpdateStopStatusRequest(@NotNull(message = "STOP_STATUS_REQUIRED") StopStatus status,
                                      @Size(max = 255, message = "INVALID_FAILURE_REASON_LENGTH") String failureReason) {}
