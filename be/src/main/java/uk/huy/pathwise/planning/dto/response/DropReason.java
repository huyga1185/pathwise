package uk.huy.pathwise.planning.dto.response;

/**
 * Why a stop has no route (P-13).
 */
public enum DropReason {
    /** The plan has no valid optimization result. */
    NOT_OPTIMIZED,
    /** The solver could not fit the stop into any route. */
    INFEASIBLE
}
