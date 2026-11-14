package uk.huy.pathwise.planning.model;

/**
 * Which stops {@code POST /plans/{id}/copy} takes (T62, P-67).
 */
public enum CopyScope {
    ALL,
    /** FAILED, SKIPPED, and stops still PENDING: what was not delivered. */
    NOT_DELIVERED
}
