package uk.huy.pathwise.core.observability;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for counts per local day computed in SQL (T60): {@code DATE(DATE_ADD(col, INTERVAL :offset SECOND))} turns
 * a UTC timestamp into the day of a fixed offset (Asia/Ho_Chi_Minh has no daylight saving).
 */
public final class DailyCounts {
    private DailyCounts() {
    }

    public static int offsetSeconds(ZoneOffset offset) {
        return offset.getTotalSeconds();
    }

    /**
     * @param rows {@code [day, count]} rows of a native query
     */
    public static Map<LocalDate, Long> byDay(List<Object[]> rows) {
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Object[] row : rows) counts.put(day(row[0]), ((Number) row[1]).longValue());
        return counts;
    }

    public static LocalDate day(Object value) {
        if (value instanceof LocalDate date) return date;
        if (value instanceof java.sql.Date date) return date.toLocalDate();
        return LocalDate.parse(value.toString());
    }
}
