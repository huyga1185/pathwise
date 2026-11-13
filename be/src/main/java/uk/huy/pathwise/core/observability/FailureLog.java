package uk.huy.pathwise.core.observability;

import org.jspecify.annotations.Nullable;

/**
 * One place to report the failures operators watch (T54, ARCHITECTURE.md §10): an email not sent, a routing or
 * geocoding server down, a job failed. Every report is one log line in a fixed format, so it can be searched in the
 * container logs and counted (T60 adds counters behind this interface).
 * <p>
 * Never pass personal data or secrets: no email address, no address searched, no token.
 */
public interface FailureLog {
    /**
     * @param source    what failed
     * @param operation which call, e.g. {@code osrm.table}, {@code smtp.send}, or the job type
     * @param reason    short, stable reason, e.g. an error code or {@code timeout}
     * @param detail    extra {@code key=value} pairs (ids), or empty
     * @param cause     the exception when its stack trace helps (an unexpected error), otherwise null
     */
    void record(FailureSource source, String operation, String reason, String detail, @Nullable Throwable cause);
}
