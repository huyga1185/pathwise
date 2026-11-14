package uk.huy.pathwise.planning.infrastructure.sheet;

import java.util.List;

/**
 * Reads the first sheet of a file as rows of cells (T61). Blank rows are skipped. Implementations wrap their library:
 * any failure to read is a {@link SheetFormatException}.
 */
public interface SheetReader {
    /**
     * @param content the whole file
     * @param maxRows stop after this many rows that are not blank; the caller treats {@code maxRows} rows as "too many"
     * @throws SheetFormatException if the file is not readable in this format
     */
    List<SheetRow> read(byte[] content, int maxRows);
}
