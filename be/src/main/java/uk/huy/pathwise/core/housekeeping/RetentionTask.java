package uk.huy.pathwise.core.housekeeping;

import java.time.Instant;

/**
 * Deletes the rows of one module that are past their retention (T53, P-59). Each module implements it for its own
 * tables; {@link HousekeepingService} finds every implementation and runs them on a schedule.
 */
public interface RetentionTask {
    /**
     * @return the key of the task in {@code housekeeping.retention.<name>}
     */
    String name();

    /**
     * Deletes what became stale before {@code cutoff} (now minus the configured retention), in its own transaction.
     *
     * @return the number of rows deleted
     */
    int purge(Instant cutoff);
}
