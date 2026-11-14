package uk.huy.pathwise.core.observability;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Writes each failure as {@code failure source=ROUTING operation=osrm.table reason=… [detail]} on the logger
 * {@value #LOGGER}: WARN, or ERROR with the stack trace when a cause is given. Find them with
 * {@code docker compose logs app | grep 'failure source='}. Each failure is also counted in {@link OpsMetrics} (T60).
 */
@Component
public class LoggingFailureLog implements FailureLog {
    static final String LOGGER = "uk.huy.pathwise.failure";
    private static final Logger log = LoggerFactory.getLogger(LOGGER);

    private final ObjectProvider<OpsMetrics> metrics;

    public LoggingFailureLog(ObjectProvider<OpsMetrics> metrics) {
        this.metrics = metrics;
    }

    @Override
    public void record(FailureSource source, String operation, String reason, String detail, @Nullable Throwable cause) {
        OpsMetrics opsMetrics = metrics.getIfAvailable();
        if (opsMetrics != null) opsMetrics.recordFailure(source);
        String suffix = detail.isBlank() ? "" : " " + detail;
        if (cause == null) log.warn("failure source={} operation={} reason={}{}", source, operation, reason, suffix);
        else log.error("failure source={} operation={} reason={}{}", source, operation, reason, suffix, cause);
    }
}
