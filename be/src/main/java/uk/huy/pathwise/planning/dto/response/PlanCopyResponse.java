package uk.huy.pathwise.planning.dto.response;

import java.util.List;

/**
 * {@code POST /plans/{id}/copy} (T62): the new draft and the assignments that could not be copied.
 */
public record PlanCopyResponse(PlanDetailResponse plan, List<SkippedAssignmentResponse> skippedAssignments) {}
