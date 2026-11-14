package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * Technical numbers (T60, FR-A16). Request, failure and rate-limit counts are those of this running instance since
 * {@code startedAt}: they start again at zero after a restart. Jobs and database come from the database.
 */
public record TechnicalMetricsResponse(Instant startedAt,
                                       long uptimeSeconds,
                                       Instant measuredAt,
                                       Http http,
                                       Jobs jobs,
                                       Failures failuresLast24h,
                                       long rateLimitedLast24h,
                                       Jvm jvm,
                                       Database database) {
    /**
     * @param requestsPerMinute average over the last 15 minutes
     * @param clientErrorRate   4xx / requests of the last hour; null without requests
     * @param serverErrorRate   5xx / requests of the last hour; null without requests
     */
    public record Http(double requestsPerMinute, long requestsLastHour,
                       @Nullable Double clientErrorRate, @Nullable Double serverErrorRate, List<Group> groups) {}

    /**
     * @param group     first path segment after {@code /api/v1}, e.g. {@code plans}
     * @param p95Millis 95th percentile of the response time over about the last hour; null when unknown
     */
    public record Group(String group, long requestsLastHour, @Nullable Double p95Millis) {}

    /**
     * @param unfinished   QUEUED or RUNNING now
     * @param failedLast24h ended FAILED in the last 24 hours (from the database, kept across restarts)
     */
    public record Jobs(long unfinished, long failedLast24h) {}

    /**
     * Failures reported by {@code FailureLog} in the last 24 hours (T54): SMTP, OSRM, Nominatim, jobs.
     */
    public record Failures(long email, long routing, long geocoding, long job) {}

    /**
     * @param heapMaxBytes null when the JVM has no limit
     */
    public record Jvm(long heapUsedBytes, @Nullable Long heapMaxBytes) {}

    /**
     * @param activeConnections null if the pool cannot be read
     */
    public record Database(@Nullable Integer activeConnections, @Nullable Integer idleConnections,
                           @Nullable Integer maxConnections, long sizeBytes, List<Table> largestTables) {}

    /**
     * @param rows estimate of the storage engine
     */
    public record Table(String name, long bytes, long rows) {}
}
