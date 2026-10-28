package uk.huy.pathwise.planning.model;

/**
 * Status of one driver's route, derived from its stops (not stored): a driver who has finished sees COMPLETED even
 * while other routes of the plan are still running.
 */
public enum RouteStatus {
    /** No stop has left PENDING yet. */
    PENDING,
    IN_PROGRESS,
    /** Every stop has finished: DELIVERED, FAILED or SKIPPED. */
    COMPLETED
}
