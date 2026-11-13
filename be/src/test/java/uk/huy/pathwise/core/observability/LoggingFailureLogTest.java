package uk.huy.pathwise.core.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class LoggingFailureLogTest {
    private final LoggingFailureLog failureLog = new LoggingFailureLog();

    @Test
    void record_whenNoCause_writeOneWarnLineInTheFixedFormat(CapturedOutput output) {
        failureLog.record(FailureSource.JOB, "GEOCODE_PLAN", "GEOCODING_PROVIDER_ERROR", "jobId=12", null);

        assertThat(output).contains("WARN")
                .contains("failure source=JOB operation=GEOCODE_PLAN reason=GEOCODING_PROVIDER_ERROR jobId=12");
    }

    @Test
    void record_whenCauseAndNoDetail_writeErrorWithStackTrace(CapturedOutput output) {
        failureLog.record(FailureSource.JOB, "OPTIMIZE_PLAN", "INTERNAL_ERROR", "", new IllegalStateException("bug"));

        assertThat(output).contains("ERROR")
                .contains("failure source=JOB operation=OPTIMIZE_PLAN reason=INTERNAL_ERROR\n")
                .contains("java.lang.IllegalStateException: bug");
    }
}
