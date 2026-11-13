package uk.huy.pathwise.planning.model;

/**
 * Constraints a route can break after a manual edit (T28, P-58); the optimizer never breaks them.
 */
public enum ViolationCode {
    /** More load than the vehicle's capacity, in its unit (D-36). */
    CAPACITY_EXCEEDED,
    /** Arrives at a stop after the end of its time window. */
    TIME_WINDOW_MISSED,
    /** Back at the end of the route after the end of the shift. */
    SHIFT_EXCEEDED,
    /** Longer than the assignment's maximum distance. */
    MAX_DISTANCE_EXCEEDED
}
