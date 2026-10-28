package uk.huy.pathwise.planning.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.huy.pathwise.planning.model.PlanStatus.*;
import static uk.huy.pathwise.planning.model.StopStatus.DELIVERED;
import static uk.huy.pathwise.planning.model.StopStatus.FAILED;
import static uk.huy.pathwise.planning.model.StopStatus.PENDING;
import static uk.huy.pathwise.planning.model.StopStatus.SKIPPED;

class PlanLifecycleTest {
    @Test
    void canMove_plan_followTheTableOneWayOnly() {
        assertThat(PlanLifecycle.canMove(DRAFT, PUBLISHED)).isTrue();
        assertThat(PlanLifecycle.canMove(DRAFT, CANCELLED)).isTrue();
        assertThat(PlanLifecycle.canMove(PUBLISHED, IN_PROGRESS)).isTrue();
        assertThat(PlanLifecycle.canMove(PUBLISHED, COMPLETED)).isTrue();
        assertThat(PlanLifecycle.canMove(PUBLISHED, CANCELLED)).isTrue();
        assertThat(PlanLifecycle.canMove(IN_PROGRESS, COMPLETED)).isTrue();

        assertThat(PlanLifecycle.canMove(DRAFT, IN_PROGRESS)).isFalse();
        assertThat(PlanLifecycle.canMove(PUBLISHED, DRAFT)).isFalse();
        assertThat(PlanLifecycle.canMove(IN_PROGRESS, CANCELLED)).isFalse();
        assertThat(PlanLifecycle.canMove(IN_PROGRESS, PUBLISHED)).isFalse();
        for (PlanStatus to : PlanStatus.values()) {
            assertThat(PlanLifecycle.canMove(COMPLETED, to)).isFalse();
            assertThat(PlanLifecycle.canMove(CANCELLED, to)).isFalse();
        }
    }

    @Test
    void canMove_stop_onlyFromPendingNeverBack() {
        assertThat(PlanLifecycle.canMove(PENDING, DELIVERED)).isTrue();
        assertThat(PlanLifecycle.canMove(PENDING, FAILED)).isTrue();
        assertThat(PlanLifecycle.canMove(PENDING, SKIPPED)).isTrue();
        for (StopStatus finished : List.of(DELIVERED, FAILED, SKIPPED)) {
            for (StopStatus to : StopStatus.values()) assertThat(PlanLifecycle.canMove(finished, to)).isFalse();
        }
    }

    @Test
    void progress_whenStopsMove_startThenComplete() {
        assertThat(PlanLifecycle.progress(PUBLISHED, List.of(PENDING, PENDING))).isEqualTo(PUBLISHED);
        assertThat(PlanLifecycle.progress(PUBLISHED, List.of(DELIVERED, PENDING))).isEqualTo(IN_PROGRESS);
        assertThat(PlanLifecycle.progress(IN_PROGRESS, List.of(DELIVERED, FAILED))).isEqualTo(COMPLETED);
        assertThat(PlanLifecycle.progress(PUBLISHED, List.of(SKIPPED))).isEqualTo(COMPLETED);
    }

    @Test
    void progress_whenNotRunningOrNoAssignedStop_keepStatus() {
        assertThat(PlanLifecycle.progress(DRAFT, List.of(DELIVERED))).isEqualTo(DRAFT);
        assertThat(PlanLifecycle.progress(CANCELLED, List.of(DELIVERED))).isEqualTo(CANCELLED);
        assertThat(PlanLifecycle.progress(COMPLETED, List.of(DELIVERED))).isEqualTo(COMPLETED);
        assertThat(PlanLifecycle.progress(PUBLISHED, List.of())).isEqualTo(PUBLISHED);
        assertThat(PlanLifecycle.isActive(PUBLISHED)).isTrue();
        assertThat(PlanLifecycle.isActive(IN_PROGRESS)).isTrue();
        assertThat(PlanLifecycle.isActive(COMPLETED)).isFalse();
    }
}
