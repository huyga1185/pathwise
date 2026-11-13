package uk.huy.pathwise.planning.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualScheduleTest {
    private static final int EIGHT = 8 * 3600;

    @Test
    void compute_whenNoTimeWindow_leaveAtShiftStartAndAddLegsAndService() {
        ManualSchedule.RouteTimes times = ManualSchedule.compute(EIGHT,
                List.of(new ManualSchedule.Visit(300, null), new ManualSchedule.Visit(120, null)),
                new int[]{600, 900, 1200}, new int[]{5000, 7000, 9000});

        assertThat(times.startSeconds()).isEqualTo(EIGHT);
        assertThat(times.serviceStarts()).containsExactly(EIGHT + 600, EIGHT + 600 + 300 + 900);
        assertThat(times.endSeconds()).isEqualTo(EIGHT + 600 + 300 + 900 + 120 + 1200);
        assertThat(times.durationSeconds()).isEqualTo(3120);
        assertThat(times.distanceMeters()).isEqualTo(21000);
    }

    @Test
    void compute_whenFirstWindowOpensLater_leaveLaterInsteadOfWaiting() {
        int nine = 9 * 3600;
        ManualSchedule.RouteTimes times = ManualSchedule.compute(EIGHT,
                List.of(new ManualSchedule.Visit(0, nine)), new int[]{600, 600}, new int[]{1, 1});

        assertThat(times.startSeconds()).isEqualTo(nine - 600);
        assertThat(times.serviceStarts()).containsExactly(nine);
        assertThat(times.durationSeconds()).isEqualTo(1200);
    }

    @Test
    void compute_whenArrivingEarlyAtLaterStop_waitForItsWindow() {
        int ten = 10 * 3600;
        ManualSchedule.RouteTimes times = ManualSchedule.compute(EIGHT,
                List.of(new ManualSchedule.Visit(0, null), new ManualSchedule.Visit(60, ten)),
                new int[]{600, 600, 600}, new int[]{1, 1, 1});

        assertThat(times.serviceStarts()).containsExactly(EIGHT + 600, ten);
        assertThat(times.endSeconds()).isEqualTo(ten + 60 + 600);
    }

    @Test
    void compute_whenFirstWindowAlreadyOpen_leaveAtShiftStart() {
        ManualSchedule.RouteTimes times = ManualSchedule.compute(EIGHT,
                List.of(new ManualSchedule.Visit(0, 7 * 3600)), new int[]{600, 600}, new int[]{1, 1});

        assertThat(times.startSeconds()).isEqualTo(EIGHT);
    }

    @Test
    void compute_whenLegsDoNotMatchVisits_throw() {
        assertThatThrownBy(() -> ManualSchedule.compute(EIGHT, List.of(new ManualSchedule.Visit(0, null)),
                new int[]{1}, new int[]{1})).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ManualSchedule.compute(EIGHT, List.of(), new int[]{1}, new int[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
