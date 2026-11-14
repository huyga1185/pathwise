package uk.huy.pathwise.core.observability;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class RollingCounterTest {
    private Instant now = Instant.parse("2026-11-14T08:00:30Z");
    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

    @Test
    void sum_whenTimeMoves_countOnlyTheWindow() {
        RollingCounter counter = new RollingCounter(clock);
        counter.increment();
        counter.increment();
        now = now.plus(Duration.ofMinutes(30));
        counter.increment();

        assertThat(counter.sum(Duration.ofMinutes(1))).isEqualTo(1);
        assertThat(counter.sum(Duration.ofMinutes(31))).isEqualTo(3);

        now = now.plus(Duration.ofHours(24));
        assertThat(counter.sum(Duration.ofHours(24))).isZero();
        // A bucket reused a day later starts from zero.
        counter.increment();
        assertThat(counter.sum(Duration.ofHours(24))).isEqualTo(1);
    }
}
