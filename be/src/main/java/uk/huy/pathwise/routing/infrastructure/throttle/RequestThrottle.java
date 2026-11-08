package uk.huy.pathwise.routing.infrastructure.throttle;

/**
 * Global limit on calls to an external API, shared by every thread and instance using the same Redis (P-11, D-25).
 */
public interface RequestThrottle {
    /**
     * Takes the next free slot, in arrival order, and waits until it starts.
     *
     * @return false if no slot was free within the configured maximum wait
     */
    boolean acquire();
}
