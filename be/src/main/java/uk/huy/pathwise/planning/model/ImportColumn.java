package uk.huy.pathwise.planning.model;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Columns of the stop import file (T61, P-68), in the order of the template. A header matches a column whatever its
 * case, accents and spacing ("Số điện thoại", "SO DIEN THOAI", "sđt"); columns can be in any order.
 */
public enum ImportColumn {
    LABEL("Nhãn / mã đơn", List.of("nhanmadon", "nhan", "madon", "label")),
    RECIPIENT_NAME("Người nhận", List.of("nguoinhan", "tennguoinhan")),
    RECIPIENT_PHONE("Số điện thoại", List.of("sodienthoai", "dienthoai", "sdt")),
    ADDRESS("Địa chỉ", List.of("diachi")),
    LAT("Vĩ độ", List.of("vido", "lat", "latitude")),
    LON("Kinh độ", List.of("kinhdo", "lon", "lng", "longitude")),
    KG("KG", List.of("kg")),
    BOX("Thùng", List.of("thung")),
    PACKAGE("Kiện", List.of("kien")),
    SERVICE_MINUTES("Phút phục vụ", List.of("phutphucvu")),
    WINDOW_START("Giao từ", List.of("giaotu")),
    WINDOW_END("Giao đến", List.of("giaoden")),
    NOTE("Ghi chú", List.of("ghichu"));

    private final String header;
    private final List<String> keys;

    ImportColumn(String header, List<String> keys) {
        this.header = header;
        this.keys = keys;
    }

    /**
     * @return the header written in the template
     */
    public String header() {
        return header;
    }

    public static Optional<ImportColumn> match(String header) {
        String key = normalize(header);
        for (ImportColumn column : values()) {
            if (column.keys.contains(key)) return Optional.of(column);
        }
        return Optional.empty();
    }

    /**
     * Lower case, without accents, {@code đ} as {@code d}, letters and digits only.
     */
    static String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd');
        return decomposed.replaceAll("[^a-z0-9]", "");
    }
}
