package uk.huy.pathwise.planning.dto.response;

import java.util.List;

/**
 * Result of reading a stop import file ({@code POST /plans/import}, T61): nothing is saved. The client adds the rows
 * without errors to its draft and saves the draft as usual.
 *
 * @param rows           every row that is not blank, in file order
 * @param validCount     rows without errors
 * @param errorCount     rows with at least one error
 * @param ignoredColumns headers that match no column of the template, or repeat one, as written in the file
 */
public record PlanImportResponse(List<ImportedRowResponse> rows,
                                 int validCount,
                                 int errorCount,
                                 List<String> ignoredColumns) {}
