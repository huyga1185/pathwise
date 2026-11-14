package uk.huy.pathwise.planning.infrastructure.sheet;

import org.jspecify.annotations.Nullable;

import java.time.LocalTime;

/**
 * One cell of an imported sheet. A CSV cell is only text; an Excel cell also keeps its typed value, so that a number or
 * a time is not read back from its display text.
 *
 * @param text   the value as text, trimmed; empty for a blank cell
 * @param number the value of a numeric cell that is not a date or time
 * @param time   the time of a cell formatted as a date or time
 */
public record SheetCell(String text, @Nullable Double number, @Nullable LocalTime time) {
    public static final SheetCell BLANK = new SheetCell("", null, null);

    public static SheetCell ofText(String text) {
        return new SheetCell(text == null ? "" : text.strip(), null, null);
    }

    public boolean isBlank() {
        return text.isEmpty() && number == null && time == null;
    }
}
