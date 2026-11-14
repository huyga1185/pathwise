package uk.huy.pathwise.planning.infrastructure.sheet;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import uk.huy.pathwise.planning.model.ImportColumn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The template files of the stop import (T61): one header row in the order of {@link ImportColumn} and one example
 * row. Every cell is text, so Excel keeps the leading 0 of the phone number and the times as typed.
 */
public class ImportTemplateWriter {
    private static final List<String> EXAMPLE = List.of("ĐH-001", "Chị Lan", "0905123456",
            "12 Lê Duẩn, Hải Châu, Đà Nẵng", "", "", "", "2", "", "5", "09:00", "11:00", "Gọi trước 10 phút");

    public byte[] xlsx() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Điểm giao");
            CellStyle text = workbook.createCellStyle();
            text.setDataFormat(workbook.createDataFormat().getFormat("@"));
            CellStyle header = workbook.createCellStyle();
            header.cloneStyleFrom(text);
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);
            ImportColumn[] columns = ImportColumn.values();
            Row headerRow = sheet.createRow(0);
            Row exampleRow = sheet.createRow(1);
            for (int i = 0; i < columns.length; i++) {
                headerRow.createCell(i).setCellValue(columns[i].header());
                headerRow.getCell(i).setCellStyle(header);
                exampleRow.createCell(i).setCellValue(EXAMPLE.get(i));
                exampleRow.getCell(i).setCellStyle(text);
                sheet.setDefaultColumnStyle(i, text);
                sheet.setColumnWidth(i, (Math.max(columns[i].header().length(), EXAMPLE.get(i).length()) + 4) * 256);
            }
            sheet.createFreezePane(0, 1);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * UTF-8 with a BOM, so Excel shows the accents.
     */
    public byte[] csv() {
        String header = Arrays.stream(ImportColumn.values()).map(ImportColumn::header).collect(Collectors.joining(","));
        String example = EXAMPLE.stream().map(ImportTemplateWriter::quote).collect(Collectors.joining(","));
        return ("﻿" + header + "\r\n" + example + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private static String quote(String value) {
        return value.contains(",") || value.contains("\"") ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
