package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.planning.model.ViolationCode;

/**
 * A constraint a stored route breaks (API_SPEC §7, T28, P-58). The plan is still valid: the dispatcher decides.
 *
 * @param stopId the late stop for {@code TIME_WINDOW_MISSED}; null for the other codes
 * @param excess by how much: capacity units of the vehicle ({@code CAPACITY_EXCEEDED}), seconds
 *               ({@code TIME_WINDOW_MISSED}, {@code SHIFT_EXCEEDED}) or metres ({@code MAX_DISTANCE_EXCEEDED})
 */
public record ViolationResponse(ViolationCode code, long routeId, @Nullable Long stopId, int excess) {}
