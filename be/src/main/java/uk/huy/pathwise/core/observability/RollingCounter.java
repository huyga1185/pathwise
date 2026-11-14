package uk.huy.pathwise.core.observability;

import java.time.Clock;
import java.time.Duration;

/**
 * Counts events per minute over the last 24 hours (T60), in memory: the window of a sum moves with the clock.
 * Thread-safe; 1440 buckets per counter.
 */
public class RollingCounter {
    static final int MINUTES = 24 * 60;

    private final Clock clock;
    private final long[] counts = new long[MINUTES];
    // The minute (epoch minutes) each bucket holds; a bucket of an older minute counts as zero.
    private final long[] minutes = new long[MINUTES];

    public RollingCounter(Clock clock) {
        this.clock = clock;
    }

    public synchronized void increment() {
        long minute = currentMinute();
        int bucket = (int) (minute % MINUTES);
        if (minutes[bucket] != minute) {
            minutes[bucket] = minute;
            counts[bucket] = 0;
        }
        counts[bucket]++;
    }

    /**
     * @param window at most 24 hours; the current minute included
     */
    public synchronized long sum(Duration window) {
        long now = currentMinute();
        long span = Math.min(MINUTES, Math.max(1, window.toMinutes()));
        long total = 0;
        for (int i = 0; i < MINUTES; i++) {
            if (now - minutes[i] < span && minutes[i] <= now) total += counts[i];
        }
        return total;
    }

    private long currentMinute() {
        return clock.millis() / 60_000;
    }
}
