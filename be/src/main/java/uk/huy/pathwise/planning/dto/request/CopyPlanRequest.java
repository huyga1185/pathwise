package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.planning.model.CopyScope;

import java.time.LocalDate;

/**
 * {@code POST /plans/{id}/copy} (T62). An unknown {@code stops} value is 400 {@code MALFORMED_REQUEST}.
 *
 * @param name optional; "{old name} (bản sao)" when missing or blank
 */
public record CopyPlanRequest(@NotNull(message = "PLAN_DATE_REQUIRED") LocalDate planDate,
                              @Size(max = 150, message = "INVALID_PLAN_NAME_LENGTH") @Nullable String name,
                              @NotNull(message = "COPY_SCOPE_REQUIRED") CopyScope stops) {}
