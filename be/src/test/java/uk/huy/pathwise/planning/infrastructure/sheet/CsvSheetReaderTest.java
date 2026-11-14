package uk.huy.pathwise.planning.infrastructure.sheet;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvSheetReaderTest {
    private final CsvSheetReader reader = new CsvSheetReader();

    @Test
    void read_whenQuotedFieldsAndBlankLines_keepLineNumbersAndSkipBlankRows() {
        String csv = "Nhãn,Ghi chú\r\n\"A, 1\",\"Nói \"\"gọi trước\"\"\"\r\n,\r\n\nB,\"hai\ndòng\"\nC,x";

        List<SheetRow> rows = reader.read(csv.getBytes(StandardCharsets.UTF_8), 100);

        assertThat(rows).extracting(SheetRow::number).containsExactly(1, 2, 5, 7);
        assertThat(texts(rows.get(1))).containsExactly("A, 1", "Nói \"gọi trước\"");
        assertThat(texts(rows.get(2))).containsExactly("B", "hai\ndòng");
        assertThat(texts(rows.get(3))).containsExactly("C", "x");
    }

    @Test
    void read_whenBomOrWindows1258_decodeAccents() {
        byte[] bom = ("﻿Nhãn;Địa chỉ\nA;Đà Nẵng").getBytes(StandardCharsets.UTF_8);
        byte[] ansi = windows1258("Nhãn;Địa chỉ\nA;Đà Nẵng");

        for (byte[] content : List.of(bom, ansi)) {
            List<SheetRow> rows = reader.read(content, 100);
            assertThat(texts(rows.get(0))).containsExactly("Nhãn", "Địa chỉ");
            assertThat(texts(rows.get(1))).containsExactly("A", "Đà Nẵng");
        }
    }

    @Test
    void read_whenMoreRowsThanMax_stopAtMax() {
        assertThat(reader.read("a\nb\nc\nd".getBytes(StandardCharsets.UTF_8), 2)).hasSize(2);
    }

    @Test
    void read_whenNulBytes_throwSheetFormatException() {
        assertThatThrownBy(() -> reader.read(new byte[]{'a', 0, 'b'}, 10)).isInstanceOf(SheetFormatException.class);
    }

    @Test
    void delimiter_whenSemicolonOrTabMoreFrequent_pickIt() {
        assertThat(CsvSheetReader.delimiter("a;b;\"c,d,e\"\nx,y,z")).isEqualTo(';');
        assertThat(CsvSheetReader.delimiter("a\tb\tc")).isEqualTo('\t');
        assertThat(CsvSheetReader.delimiter("a")).isEqualTo(',');
    }

    /**
     * As Excel saves "CSV" on a Vietnamese Windows: base letters of the code page (ă, â, ê, ô, ơ, ư, đ) precomposed, tone
     * marks as combining characters.
     */
    private static byte[] windows1258(String text) {
        String decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replace("a\u0306", "ă").replace("a\u0302", "â").replace("e\u0302", "ê").replace("o\u0302", "ô")
                .replace("o\u031B", "ơ").replace("u\u031B", "ư");
        byte[] bytes = decomposed.getBytes(Charset.forName("windows-1258"));
        assertThat(new String(bytes, Charset.forName("windows-1258"))).doesNotContain("?");
        return bytes;
    }

    private static List<String> texts(SheetRow row) {
        return row.cells().stream().map(SheetCell::text).toList();
    }
}
