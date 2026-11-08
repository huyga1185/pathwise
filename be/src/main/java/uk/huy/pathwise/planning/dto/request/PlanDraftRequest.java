package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * A whole plan draft ({@code POST /plans}, {@code PUT /plans/{id}}, API_SPEC §7). The size limits (D-19) answer 422
 * {@code PLAN_TOO_LARGE}, so they are checked by the service, not here.
 *
 * @param version     the version last read; required by {@code PUT} ({@link Replace}), ignored by {@code POST}
 * @param depot       optional; the store depot by default (D-20)
 * @param stops       optional, empty by default
 * @param assignments optional, empty by default
 */
public record PlanDraftRequest(@NotNull(groups = Replace.class, message = "VERSION_REQUIRED") Long version,
                               @NotBlank(message = "PLAN_NAME_REQUIRED") @Size(max = 150, message = "INVALID_PLAN_NAME_LENGTH") String name,
                               @NotNull(message = "PLAN_DATE_REQUIRED") LocalDate planDate,
                               @Valid DepotRequest depot,
                               List<@Valid @NotNull(message = "STOP_REQUIRED") PlanStopRequest> stops,
                               List<@Valid @NotNull(message = "ASSIGNMENT_REQUIRED") AssignmentRequest> assignments) {
    /**
     * Validation group of {@code PUT}, which replaces an existing draft.
     */
    public interface Replace {}

    public List<PlanStopRequest> stopsOrEmpty() {
        return stops == null ? List.of() : stops;
    }

    public List<AssignmentRequest> assignmentsOrEmpty() {
        return assignments == null ? List.of() : assignments;
    }
}
