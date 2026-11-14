package uk.huy.pathwise.planning.infrastructure.sheet;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * CSV as Excel and spreadsheets write it (RFC 4180: quoted fields, {@code ""} inside quotes, line breaks in quotes).
 * UTF-8 with or without BOM; bytes that are not valid UTF-8 are read as Windows-1258, the encoding Vietnamese Excel
 * uses for plain "CSV" (P-68); the text is then NFC. The delimiter is the most frequent of {@code ,} {@code ;} and tab
 * in the first line.
 */
public class CsvSheetReader implements SheetReader {
    private static final Charset WINDOWS_1258 = Charset.forName("windows-1258");

    @Override
    public List<SheetRow> read(byte[] content, int maxRows) {
        String text = decode(content);
        if (text.indexOf('\0') >= 0) throw new SheetFormatException("CSV contains NUL bytes");
        char delimiter = delimiter(text);
        List<SheetRow> rows = new ArrayList<>();
        List<SheetCell> cells = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        int line = 1;
        int recordLine = 1;
        int i = 0;
        while (i < text.length() && rows.size() < maxRows) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i += 2;
                    continue;
                }
                if (c == '"') quoted = false;
                else {
                    if (c == '\n') line++;
                    field.append(c);
                }
                i++;
                continue;
            }
            if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == delimiter) {
                cells.add(SheetCell.ofText(field.toString()));
                field.setLength(0);
            } else if (c == '\r' || c == '\n') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                cells.add(SheetCell.ofText(field.toString()));
                field.setLength(0);
                addIfNotBlank(rows, recordLine, cells);
                cells = new ArrayList<>();
                line++;
                recordLine = line;
            } else {
                field.append(c);
            }
            i++;
        }
        if (rows.size() < maxRows && (!field.isEmpty() || !cells.isEmpty())) {
            cells.add(SheetCell.ofText(field.toString()));
            addIfNotBlank(rows, recordLine, cells);
        }
        return rows;
    }

    private static void addIfNotBlank(List<SheetRow> rows, int number, List<SheetCell> cells) {
        if (cells.stream().anyMatch(cell -> !cell.isBlank())) rows.add(new SheetRow(number, cells));
    }

    static String decode(byte[] content) {
        int offset = content.length >= 3 && (content[0] & 0xFF) == 0xEF && (content[1] & 0xFF) == 0xBB
                && (content[2] & 0xFF) == 0xBF ? 3 : 0;
        ByteBuffer bytes = ByteBuffer.wrap(content, offset, content.length - offset);
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
        } catch (CharacterCodingException e) {
            text = new String(content, offset, content.length - offset, WINDOWS_1258);
        }
        // Windows-1258 writes tone marks as combining characters ("a" + U+0303); one form for every comparison.
        return Normalizer.normalize(text, Normalizer.Form.NFC);
    }

    /**
     * The most frequent of {@code ,} {@code ;} and tab outside quotes in the first line; {@code ,} on a tie or none.
     */
    static char delimiter(String text) {
        int commas = 0;
        int semicolons = 0;
        int tabs = 0;
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') quoted = !quoted;
            else if (!quoted && (c == '\n' || c == '\r')) break;
            else if (!quoted && c == ',') commas++;
            else if (!quoted && c == ';') semicolons++;
            else if (!quoted && c == '\t') tabs++;
        }
        if (semicolons > commas && semicolons >= tabs) return ';';
        if (tabs > commas && tabs > semicolons) return '\t';
        return ',';
    }
}
