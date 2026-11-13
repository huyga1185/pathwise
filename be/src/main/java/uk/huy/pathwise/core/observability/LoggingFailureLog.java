package uk.huy.pathwise.core.observability;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes each failure as {@code failure source=ROUTING operation=osrm.table reason=… [detail]} on the logger
 * {@value #LOGGER}: WARN, or ERROR with the stack trace when a cause is given. Find them with
 * {@code docker compose logs app | grep 'failure source='}.
 */
@Component
public class LoggingFailureLog implements FailureLog {
    static final String LOGGER = "uk.huy.pathwise.failure";
    private static final Logger log = LoggerFactory.getLogger(LOGGER);

    @Override
    public void record(FailureSource source, String operation, String reason, String detail, @Nullable Throwable cause) {
        String suffix = detail.isBlank() ? "" : " " + detail;
        if (cause == null) log.warn("failure source={} operation={} reason={}{}", source, operation, reason, suffix);
        else log.error("failure source={} operation={} reason={}{}", source, operation, reason, suffix, cause);
    }
}
