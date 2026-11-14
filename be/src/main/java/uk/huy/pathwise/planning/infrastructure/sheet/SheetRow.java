package uk.huy.pathwise.planning.infrastructure.sheet;

import java.util.List;

/**
 * A row that is not blank.
 *
 * @param number the row number the user sees: 1 for the first row of the sheet or the first line of the CSV
 */
public record SheetRow(int number, List<SheetCell> cells) {
    public SheetRow {
        cells = List.copyOf(cells);
    }

    /**
     * @return the cell at {@code index}, blank past the end of the row
     */
    public SheetCell cell(int index) {
        return index < cells.size() ? cells.get(index) : SheetCell.BLANK;
    }
}
