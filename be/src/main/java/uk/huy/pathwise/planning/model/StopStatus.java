package uk.huy.pathwise.planning.model;

/**
 * Delivery status of a stop (BR-10): {@code PENDING -> DELIVERED | FAILED | SKIPPED}.
 */
public enum StopStatus {
    PENDING,
    DELIVERED,
    FAILED,
    SKIPPED
}
