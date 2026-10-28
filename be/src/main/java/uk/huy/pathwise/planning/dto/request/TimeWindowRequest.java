package uk.huy.pathwise.planning.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Local times in the store timezone (P-14).
 */
public record TimeWindowRequest(@NotNull(message = "TIME_WINDOW_START_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime start,
                                @NotNull(message = "TIME_WINDOW_END_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime end) {
    @AssertTrue(message = "INVALID_TIME_WINDOW")
    public boolean isOrdered() {
        return start == null || end == null || start.isBefore(end);
    }
}
