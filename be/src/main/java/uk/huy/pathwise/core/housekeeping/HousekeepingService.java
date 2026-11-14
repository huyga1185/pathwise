package uk.huy.pathwise.core.housekeeping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Runs every {@link RetentionTask} with its configured retention (T53, P-59), under a lock so that only one instance
 * runs at a time. A task without a retention is skipped with a warning; a task that fails does not stop the others.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HousekeepingService {
    private final List<RetentionTask> tasks;
    private final HousekeepingProperties properties;
    private final HousekeepingLock lock;

    /**
     * @return rows deleted by task name; empty if another instance was running (or there is no task)
     */
    public Map<String, Integer> run() {
        String token = lock.tryAcquire(properties.lockTtl());
        if (token == null) {
            log.info("Housekeeping skipped: another instance is running it");
            return Map.of();
        }
        try {
            Instant now = Instant.now();
            Map<String, Integer> deleted = new LinkedHashMap<>();
            for (RetentionTask task : tasks) {
                Duration retention = properties.retention().get(task.name());
                if (retention == null) {
                    log.warn("Housekeeping: no housekeeping.retention.{}, task skipped", task.name());
                    continue;
                }
                try {
                    int rows = task.purge(now.minus(retention));
                    deleted.put(task.name(), rows);
                    log.info("Housekeeping {}: deleted {} rows older than {}", task.name(), rows, retention);
                } catch (RuntimeException e) {
                    log.error("Housekeeping {} failed", task.name(), e);
                }
            }
            return deleted;
        } finally {
            lock.release(token);
        }
    }
}
