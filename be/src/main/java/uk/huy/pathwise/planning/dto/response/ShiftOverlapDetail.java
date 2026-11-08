package uk.huy.pathwise.planning.dto.response;

/**
 * One entry of {@code details} for 422 {@code SHIFT_OVERLAP} (D-23, API_SPEC §7).
 *
 * @param field          {@code assignments[i].driverId} or {@code assignments[i].vehicleId}, {@code i} as in
 *                       {@code PlanDetail.routes}
 * @param conflictPlanId the published plan whose shift overlaps
 */
public record ShiftOverlapDetail(String field, String code, long conflictPlanId) {
    public static final String CODE = "SHIFT_OVERLAP";
}
