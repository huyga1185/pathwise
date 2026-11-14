package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.core.exception.FieldErrorDetail;

import java.util.List;

/**
 * One row of an import file.
 *
 * @param row    row number as the user sees it in the file (1 is the header row)
 * @param stop   what could be read from the row, as a stop of {@code PlanDraftRequest}
 * @param errors the same field codes as {@code PlanDraftRequest} validation, with {@code field} the stop field
 *               ({@code label}, {@code demands}, {@code timeWindow.end}…); empty when the row can be added
 */
public record ImportedRowResponse(int row, ImportedStopResponse stop, List<FieldErrorDetail> errors) {}
