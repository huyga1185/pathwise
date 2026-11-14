package uk.huy.pathwise.planning.infrastructure.sheet;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class XlsxSheetReaderTest {
    private final XlsxSheetReader reader = new XlsxSheetReader();

    @Test
    void read_whenTypedCells_keepNumbersTimesAndCachedFormulaValues() throws Exception {
        byte[] file;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet();
            CellStyle time = workbook.createCellStyle();
            time.setDataFormat(workbook.createDataFormat().getFormat("hh:mm"));
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Nhãn");
            sheet.createRow(2); // a blank row in between
            Row row = sheet.createRow(3);
            row.createCell(0).setCellValue("  ĐH-1  ");
            row.createCell(1).setCellValue(905123456d);
            row.createCell(2).setCellValue(9.5 / 24);
            row.getCell(2).setCellStyle(time);
            row.createCell(3).setCellFormula("1+1");
            workbook.setForceFormulaRecalculation(false);
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
            file = out.toByteArray();
        }

        List<SheetRow> rows = reader.read(file, 10);

        assertThat(rows).extracting(SheetRow::number).containsExactly(1, 4);
        SheetRow row = rows.get(1);
        assertThat(row.cell(0).text()).isEqualTo("ĐH-1");
        assertThat(row.cell(1).number()).isEqualTo(905123456d);
        assertThat(row.cell(1).text()).isEqualTo("905123456");
        assertThat(row.cell(2).time()).isEqualTo(LocalTime.of(9, 30));
        assertThat(row.cell(3).number()).isEqualTo(2d);
        assertThat(row.cell(9).isBlank()).isTrue();
    }

    @Test
    void read_whenNotAZipOrOldXls_throwSheetFormatException() {
        byte[] oldXls = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
        assertThatThrownBy(() -> reader.read(oldXls, 10)).isInstanceOf(SheetFormatException.class);
        assertThatThrownBy(() -> reader.read("Nhãn\nA".getBytes(), 10)).isInstanceOf(SheetFormatException.class);
    }

    @Test
    void read_whenZipInflatesPastLimit_throwBeforePoiLoadsIt() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
            byte[] zeros = new byte[1024 * 1024];
            for (int i = 0; i < 11; i++) zip.write(zeros);
            zip.closeEntry();
        }
        assertThat(out.size()).isLessThan(100_000);

        assertThatThrownBy(() -> reader.read(out.toByteArray(), 10))
                .isInstanceOf(SheetFormatException.class)
                .hasMessageContaining("too big");
    }
}
