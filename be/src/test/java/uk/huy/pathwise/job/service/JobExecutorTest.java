package uk.huy.pathwise.job.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.infrastructure.config.JobMode;
import uk.huy.pathwise.job.infrastructure.config.JobProperties;
import uk.huy.pathwise.job.model.JobErrors;
import uk.huy.pathwise.job.model.JobType;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class JobExecutorTest {
    private final JobStateService state = mock(JobStateService.class);
    private final JobHandler handler = mock(JobHandler.class);
    private JobExecutor executor;

    @AfterEach
    void tearDown() {
        if (executor != null) executor.shutdown();
    }

    @Test
    void submit_whenQueueFull_failJobQueueFullAndAbandonIt() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        when(handler.type()).thenReturn(JobType.GEOCODE_PLAN);
        when(state.markRunning(anyLong())).thenReturn(true);
        doAnswer(invocation -> {
            started.countDown();
            release.await(10, TimeUnit.SECONDS);
            return null;
        }).when(handler).run(any());
        executor = new JobExecutor(properties(JobMode.ASYNC, 1, 1), state, List.of(handler));
        JobRef running = ref(1);
        JobRef queued = ref(2);
        JobRef rejected = ref(3);

        executor.submit(running);
        started.await(10, TimeUnit.SECONDS);
        executor.submit(queued);
        executor.submit(rejected);

        verify(state).markFailed(3, JobErrors.JOB_QUEUE_FULL);
        verify(handler).abandon(rejected);
        release.countDown();
        verify(state, timeout(5000)).markSucceeded(1);
        verify(state, timeout(5000)).markSucceeded(2);
        verify(handler, never()).abandon(running);
    }

    @Test
    void submit_whenInlineHandlerFails_failWithProviderCodeOrInternalError() {
        when(handler.type()).thenReturn(JobType.GEOCODE_PLAN);
        when(state.markRunning(anyLong())).thenReturn(true);
        executor = new JobExecutor(properties(JobMode.INLINE, 1, 1), state, List.of(handler));
        doThrow(new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR, "down", null)).when(handler).run(ref(1));
        doThrow(new IllegalStateException("bug")).when(handler).run(ref(2));

        executor.submit(ref(1));
        executor.submit(ref(2));

        verify(state).markFailed(1, "ROUTING_PROVIDER_ERROR");
        verify(state).markFailed(2, JobErrors.INTERNAL_ERROR);
        verify(state, never()).markSucceeded(anyLong());
    }

    @Test
    void submit_whenJobNoLongerQueued_doNotRunIt() {
        when(handler.type()).thenReturn(JobType.GEOCODE_PLAN);
        when(state.markRunning(1)).thenReturn(false);
        executor = new JobExecutor(properties(JobMode.INLINE, 1, 1), state, List.of(handler));

        executor.submit(ref(1));

        verify(handler, never()).run(any());
        verify(state, never()).markSucceeded(anyLong());
    }

    @Test
    void submit_whenNoHandlerOrNoMode_throwIllegalState() {
        executor = new JobExecutor(new JobProperties(null, null, new JobProperties.Executor(1, 1)), state, List.of());

        assertThatThrownBy(() -> executor.submit(ref(1))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> executor.handlerOf(JobType.OPTIMIZE_PLAN)).isInstanceOf(IllegalStateException.class);
    }

    private static JobRef ref(long id) {
        return new JobRef(id, JobType.GEOCODE_PLAN, 1, 10 + id);
    }

    private static JobProperties properties(JobMode geocodeMode, int poolSize, int queueCapacity) {
        return new JobProperties(new JobProperties.TypeSettings(geocodeMode), new JobProperties.TypeSettings(JobMode.INLINE),
                new JobProperties.Executor(poolSize, queueCapacity));
    }
}
