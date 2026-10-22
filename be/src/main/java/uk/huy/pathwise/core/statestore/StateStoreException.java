package uk.huy.pathwise.core.statestore;

/**
 * Exception thrown when an operation on a {@link StateStore} fails.
 * <p>
 * This is an unchecked exception, typically indicating an underlying infrastructure
 * issue such as a connection failure, timeout, or data access error with the
 * persistence layer (e.g., Redis, Database).
 * </p>
 * <p>
 * It acts as a generic wrapper to prevent underlying technology-specific exceptions
 * (like Spring's {@code DataAccessException}) from leaking into the business logic.
 * </p>
 */
public class StateStoreException extends RuntimeException {
    /**
     * Constructs a new {@code StateStoreException} with the specified detail message.
     *
     * @param msg the detail message (which is saved for later retrieval by the
     *            {@link #getMessage()} method)
     */
    public StateStoreException(String msg) {
        super(msg);
    }
    /**
     * Constructs a new {@code StateStoreException} with the specified detail message
     * and root cause.
     * <p>
     * This constructor is highly recommended when catching infrastructure exceptions,
     * as it preserves the original stack trace (exception chaining) for easier debugging.
     * </p>
     *
     * @param msg   the detail message (which is saved for later retrieval by the
     *              {@link #getMessage()} method)
     * @param cause the root cause (which is saved for later retrieval by the
     *              {@link #getCause()} method). A {@code null} value is permitted,
     *              and indicates that the cause is nonexistent or unknown.
     */
    public StateStoreException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
