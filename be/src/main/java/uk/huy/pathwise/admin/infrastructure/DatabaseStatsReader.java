package uk.huy.pathwise.admin.infrastructure;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.admin.dto.response.TechnicalMetricsResponse.Database;
import uk.huy.pathwise.admin.dto.response.TechnicalMetricsResponse.Table;

import javax.sql.DataSource;
import java.util.List;

/**
 * Size of the database and state of the connection pool (T60): {@code information_schema} of the current schema,
 * Hikari's pool. No store data is read.
 */
@Component
@RequiredArgsConstructor
public class DatabaseStatsReader {
    private static final int LARGEST = 5;

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public Database read() {
        List<Table> tables = jdbcTemplate.query(
                "SELECT table_name, COALESCE(data_length, 0) + COALESCE(index_length, 0) AS bytes, COALESCE(table_rows, 0) "
                        + "FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY bytes DESC",
                (row, i) -> new Table(row.getString(1), row.getLong(2), row.getLong(3)));
        long size = tables.stream().mapToLong(Table::bytes).sum();
        HikariPoolMXBean pool = dataSource instanceof HikariDataSource hikari ? hikari.getHikariPoolMXBean() : null;
        Integer max = dataSource instanceof HikariDataSource hikari ? hikari.getMaximumPoolSize() : null;
        return new Database(pool == null ? null : pool.getActiveConnections(), pool == null ? null : pool.getIdleConnections(),
                max, size, tables.stream().limit(LARGEST).toList());
    }
}
