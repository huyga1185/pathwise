package uk.huy.pathwise.planning.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import uk.huy.pathwise.core.validation.FieldErrors;
import uk.huy.pathwise.core.validation.SelfValidated;
import uk.huy.pathwise.core.validation.SelfValidating;

import java.time.LocalTime;

/**
 * Local times in the store timezone (P-14).
 */
@SelfValidated
public record TimeWindowRequest(@NotNull(message = "TIME_WINDOW_START_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime start,
                                @NotNull(message = "TIME_WINDOW_END_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime end)
        implements SelfValidating {
    @Override
    public void validate(FieldErrors errors) {
        if (start != null && end != null && !start.isBefore(end)) errors.reject("end", "INVALID_TIME_WINDOW");
    }
}
