package uk.huy.pathwise.planning.infrastructure.sheet;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The first sheet of an {@code .xlsx} file, read with Apache POI (T61, P-67). Formulas are not evaluated: a formula
 * cell gives the value Excel saved with it. Before POI builds the workbook in memory, the ZIP is inflated once and
 * refused past {@value #MAX_UNCOMPRESSED_BYTES} bytes, so a small file cannot take the heap of a 2 GB VPS.
 */
public class XlsxSheetReader implements SheetReader {
    static final long MAX_UNCOMPRESSED_BYTES = 10L * 1024 * 1024;
    private static final int MAX_ENTRIES = 1_000;
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<SheetRow> read(byte[] content, int maxRows) {
        checkUncompressedSize(content);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            List<SheetRow> rows = new ArrayList<>();
            if (workbook.getNumberOfSheets() == 0) return rows;
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (rows.size() >= maxRows) break;
                List<SheetCell> cells = new ArrayList<>();
                for (int column = 0; column < Math.max(row.getLastCellNum(), 0); column++) {
                    cells.add(toCell(row.getCell(column)));
                }
                if (cells.stream().anyMatch(cell -> !cell.isBlank())) rows.add(new SheetRow(row.getRowNum() + 1, cells));
            }
            return rows;
        } catch (IOException | RuntimeException e) {
            // POI reports a broken file with many runtime exceptions; none of them is a bug of ours.
            throw new SheetFormatException("Not a readable .xlsx file", e);
        }
    }

    private static SheetCell toCell(Cell cell) {
        if (cell == null) return SheetCell.BLANK;
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalTime time = cell.getLocalDateTimeCellValue().toLocalTime();
                    yield new SheetCell(time.format(HH_MM), null, time);
                }
                double number = cell.getNumericCellValue();
                yield new SheetCell(BigDecimal.valueOf(number).stripTrailingZeros().toPlainString(), number, null);
            }
            case STRING -> SheetCell.ofText(cell.getStringCellValue());
            case BOOLEAN -> SheetCell.ofText(String.valueOf(cell.getBooleanCellValue()));
            default -> SheetCell.BLANK;
        };
    }

    private static void checkUncompressedSize(byte[] content) {
        long total = 0;
        int entries = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new SheetFormatException("Too many entries in the .xlsx file");
                total += drain(zip, buffer, MAX_UNCOMPRESSED_BYTES - total);
                if (total > MAX_UNCOMPRESSED_BYTES) throw new SheetFormatException("The .xlsx file is too big uncompressed");
            }
        } catch (IOException e) {
            throw new SheetFormatException("Not a readable .xlsx file", e);
        }
        if (entries == 0) throw new SheetFormatException("Not a ZIP file");
    }

    /**
     * @return bytes read, at most {@code limit + 1}
     */
    private static long drain(InputStream in, byte[] buffer, long limit) throws IOException {
        long read = 0;
        int n;
        while (read <= limit && (n = in.read(buffer)) != -1) read += n;
        return read;
    }
}
