package uk.huy.pathwise.core.housekeeping;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the housekeeping at {@code housekeeping.cron} (03:30 in Vietnam by default, after the 02:30 backup of T52).
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(HousekeepingProperties.class)
@RequiredArgsConstructor
class HousekeepingScheduler {
    private final HousekeepingService housekeepingService;

    @Scheduled(cron = "${housekeeping.cron:0 30 3 * * *}", zone = "${housekeeping.zone:Asia/Ho_Chi_Minh}")
    void runNightly() {
        housekeepingService.run();
    }
}
