package uk.huy.pathwise.core.housekeeping;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.Map;

/**
 * {@code housekeeping.*} (T53).
 *
 * @param cron      when to run, Spring cron in {@code zone}; {@code -} turns the schedule off (tests)
 * @param lockTtl   how long the run keeps the lock that stops a second instance; longer than a run takes
 * @param retention how long to keep stale rows, by {@link RetentionTask#name()}
 */
@ConfigurationProperties("housekeeping")
public record HousekeepingProperties(@DefaultValue("0 30 3 * * *") String cron,
                                     @DefaultValue("Asia/Ho_Chi_Minh") String zone,
                                     @DefaultValue("30m") Duration lockTtl,
                                     Map<String, Duration> retention) {
    public HousekeepingProperties {
        retention = retention == null ? Map.of() : Map.copyOf(retention);
    }
}
