package uk.huy.pathwise.core.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingFailureLogTest {
    private final OpsMetrics opsMetrics = new OpsMetrics(new SimpleMeterRegistry());
    private final LoggingFailureLog failureLog = new LoggingFailureLog(new StaticListableBeanFactory(Map.of("opsMetrics", opsMetrics))
            .getBeanProvider(OpsMetrics.class));

    @Test
    void record_whenNoCause_writeOneWarnLineInTheFixedFormat(CapturedOutput output) {
        failureLog.record(FailureSource.JOB, "GEOCODE_PLAN", "GEOCODING_PROVIDER_ERROR", "jobId=12", null);

        assertThat(output).contains("WARN")
                .contains("failure source=JOB operation=GEOCODE_PLAN reason=GEOCODING_PROVIDER_ERROR jobId=12");
        // T60: counted for the ADMIN metrics.
        assertThat(opsMetrics.failures(FailureSource.JOB, Duration.ofHours(24))).isEqualTo(1);
        assertThat(opsMetrics.failures(FailureSource.EMAIL, Duration.ofHours(24))).isZero();
    }

    @Test
    void record_whenCauseAndNoDetail_writeErrorWithStackTrace(CapturedOutput output) {
        failureLog.record(FailureSource.JOB, "OPTIMIZE_PLAN", "INTERNAL_ERROR", "", new IllegalStateException("bug"));

        assertThat(output).contains("ERROR")
                .contains("failure source=JOB operation=OPTIMIZE_PLAN reason=INTERNAL_ERROR\n")
                .contains("java.lang.IllegalStateException: bug");
    }
}
