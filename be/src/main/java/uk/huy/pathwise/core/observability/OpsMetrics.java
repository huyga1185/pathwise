package uk.huy.pathwise.core.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Technical counters of this instance (T60), for {@code GET /admin/metrics/technical}: requests, failures and rate
 * limits per minute over the last 24 hours ({@link RollingCounter}), and the response time of each endpoint group as a
 * Micrometer timer whose p95 covers about the last hour. Everything starts again at zero when the app restarts.
 */
@Component
public class OpsMetrics {
    static final Duration P95_WINDOW = Duration.ofHours(1);

    private final MeterRegistry meterRegistry;
    private final Clock clock;
    private final Instant startedAt;
    private final Map<FailureSource, RollingCounter> failures = new EnumMap<>(FailureSource.class);
    private final RollingCounter rateLimited;
    private final RollingCounter requests;
    private final RollingCounter clientErrors;
    private final RollingCounter serverErrors;
    private final Map<String, RollingCounter> groupRequests = new ConcurrentHashMap<>();
    private final Map<String, Timer> groupTimers = new ConcurrentHashMap<>();

    @Autowired
    public OpsMetrics(MeterRegistry meterRegistry) {
        this(meterRegistry, Clock.systemUTC());
    }

    OpsMetrics(MeterRegistry meterRegistry, Clock clock) {
        this.meterRegistry = meterRegistry;
        this.clock = clock;
        this.startedAt = clock.instant();
        for (FailureSource source : FailureSource.values()) failures.put(source, new RollingCounter(clock));
        rateLimited = new RollingCounter(clock);
        requests = new RollingCounter(clock);
        clientErrors = new RollingCounter(clock);
        serverErrors = new RollingCounter(clock);
    }

    public void recordFailure(FailureSource source) {
        failures.get(source).increment();
    }

    public void recordRateLimited() {
        rateLimited.increment();
    }

    /**
     * @param group first path segment after {@code /api/v1}, e.g. {@code plans}
     */
    public void recordRequest(String group, int status, long durationNanos) {
        requests.increment();
        if (status >= 500) serverErrors.increment();
        else if (status >= 400) clientErrors.increment();
        groupRequests.computeIfAbsent(group, key -> new RollingCounter(clock)).increment();
        groupTimers.computeIfAbsent(group, key -> Timer.builder("pathwise.http.requests")
                .description("API response time by endpoint group (T60)")
                .tag("group", key)
                .publishPercentiles(0.95)
                .distributionStatisticExpiry(P95_WINDOW)
                .distributionStatisticBufferLength(6)
                .register(meterRegistry)).record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public long failures(FailureSource source, Duration window) {
        return failures.get(source).sum(window);
    }

    public long rateLimited(Duration window) {
        return rateLimited.sum(window);
    }

    public long requests(Duration window) {
        return requests.sum(window);
    }

    public long clientErrors(Duration window) {
        return clientErrors.sum(window);
    }

    public long serverErrors(Duration window) {
        return serverErrors.sum(window);
    }

    /**
     * @return by group name: requests in {@code window} and p95 in milliseconds over about the last hour (null when
     * not enough requests yet)
     */
    public Map<String, GroupStats> groups(Duration window) {
        Map<String, GroupStats> stats = new TreeMap<>();
        groupRequests.forEach((group, counter) -> stats.put(group, new GroupStats(counter.sum(window), p95(group))));
        return stats;
    }

    private @Nullable Double p95(String group) {
        Timer timer = groupTimers.get(group);
        if (timer == null) return null;
        for (ValueAtPercentile value : timer.takeSnapshot().percentileValues()) {
            if (value.percentile() == 0.95) return Math.round(value.value(TimeUnit.MILLISECONDS) * 10) / 10.0;
        }
        return null;
    }

    public record GroupStats(long requests, @Nullable Double p95Millis) {}
}
