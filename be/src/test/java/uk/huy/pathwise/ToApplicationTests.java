package uk.huy.pathwise;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import uk.huy.pathwise.support.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class ToApplicationTests extends AbstractIntegrationTest {
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void contextLoads_whenStarted_applyFlywayMigrations() {
		Integer applied = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = 1", Integer.class);

		assertThat(applied).isEqualTo(1);
	}
}
