package uk.huy.pathwise.planning;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T63 acceptance: {@code GET /reports/summary} and {@code GET /reports/stops.csv} (P-67, P-68). Plans are optimized
 * inline with the fake matrix and published; stop results are written straight to the table.
 */
@AutoConfigureMockMvc
class StoreReportIntegrationTest extends AbstractIntegrationTest {
    private static final String PLANS = "/api/v1/plans";
    private static final String REPORTS = "/api/v1/reports";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;
    private TestStore store;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void summary_whenPlansOfManyStatuses_countOnlyWorkGivenToDrivers() throws Exception {
        long vehicle = vehicle("Xe 1");
        long published = publishedPlan("2026-12-01", vehicle, "A", "B", "C", "D");
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'DELIVERED', status_updated_at = '2026-12-01 02:00:00' "
                + "WHERE plan_id = ? AND label = 'A'", published);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'FAILED', failure_reason = '  Vắng nhà ', "
                + "status_updated_at = '2026-12-01 03:30:00' WHERE plan_id = ? AND label = 'B'", published);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'FAILED', failure_reason = 'vắng nhà', "
                + "status_updated_at = '2026-12-01 03:00:00' WHERE plan_id = ? AND label = 'C'", published);
        jdbcTemplate.update("UPDATE route_plans SET status = 'IN_PROGRESS' WHERE id = ?", published);
        long cancelled = publishedPlan("2026-12-02", vehicle("Xe 2"), "E");
        jdbcTemplate.update("UPDATE route_plans SET status = 'CANCELLED' WHERE id = ?", cancelled);
        createPlan("2026-12-02", "[{\"label\":\"F\",\"lat\":16.06,\"lon\":108.21}]", "");
        createPlan("2026-12-05", "[{\"label\":\"G\",\"lat\":16.06,\"lon\":108.21}]", "");

        summary(store.dispatcher(), "?from=2026-12-01&to=2026-12-03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-12-01"))
                .andExpect(jsonPath("$.data.to").value("2026-12-03"))
                .andExpect(jsonPath("$.data.plans.total").value(3))
                .andExpect(jsonPath("$.data.plans.inProgress").value(1))
                .andExpect(jsonPath("$.data.plans.cancelled").value(1))
                .andExpect(jsonPath("$.data.plans.draft").value(1))
                .andExpect(jsonPath("$.data.stops.total").value(4))
                .andExpect(jsonPath("$.data.stops.delivered").value(1))
                .andExpect(jsonPath("$.data.stops.failed").value(2))
                .andExpect(jsonPath("$.data.stops.pending").value(1))
                .andExpect(jsonPath("$.data.stops.deliveredRate").value(1.0 / 3))
                .andExpect(jsonPath("$.data.days", hasSize(3)))
                .andExpect(jsonPath("$.data.days[0].plans").value(1))
                .andExpect(jsonPath("$.data.days[0].failed").value(2))
                .andExpect(jsonPath("$.data.days[1].plans").value(0))
                .andExpect(jsonPath("$.data.days[2].date").value("2026-12-03"))
                .andExpect(jsonPath("$.data.drivers", hasSize(1)))
                .andExpect(jsonPath("$.data.drivers[0].driverId").value(store.driver().id()))
                .andExpect(jsonPath("$.data.drivers[0].routes").value(1))
                .andExpect(jsonPath("$.data.drivers[0].delivered").value(1))
                .andExpect(jsonPath("$.data.drivers[0].firstResultAt").value("2026-12-01T02:00:00Z"))
                .andExpect(jsonPath("$.data.drivers[0].lastResultAt").value("2026-12-01T03:30:00Z"))
                .andExpect(jsonPath("$.data.failureReasons", hasSize(1)))
                .andExpect(jsonPath("$.data.failureReasons[0].count").value(2));
        String body = summary(store.owner(), "?from=2026-12-01&to=2026-12-01").andReturn().getResponse().getContentAsString();
        assertThat(((Number) JsonPath.read(body, "$.data.plannedDistanceMeters")).longValue()).isPositive();
        assertThat(((Number) JsonPath.read(body, "$.data.drivers[0].plannedDistanceMeters")).longValue())
                .isEqualTo(((Number) JsonPath.read(body, "$.data.plannedDistanceMeters")).longValue());
    }

    @Test
    void summary_whenNoPlansOrBadPeriod_emptyReportOr400() throws Exception {
        summary(store.owner(), "?from=2026-01-10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.to").value("2026-01-10"))
                .andExpect(jsonPath("$.data.days", hasSize(1)))
                .andExpect(jsonPath("$.data.stops.deliveredRate").value(nullValue()))
                .andExpect(jsonPath("$.data.drivers", hasSize(0)));
        summary(store.owner(), "").andExpect(status().isOk()).andExpect(jsonPath("$.data.days", hasSize(1)));
        summary(store.owner(), "?from=2026-01-01&to=2026-01-31").andExpect(status().isOk());

        summary(store.owner(), "?from=2026-01-01&to=2026-02-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("from"))
                .andExpect(jsonPath("$.details[0].code").value("DATE_RANGE_TOO_LONG"));
        summary(store.owner(), "?from=2026-01-02&to=2026-01-01")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("INVALID_DATE_RANGE"));
        mockMvc.perform(get(REPORTS + "/stops.csv?from=2026-01-01&to=2026-03-01").header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("DATE_RANGE_TOO_LONG"));
    }

    @Test
    void stopsCsv_whenStopsOfPlans_oneRowPerStopWithBomQuotingAndNoFormulas() throws Exception {
        long published = publishedPlan("2026-12-01", vehicle("Xe \"Một\""), "=HYPERLINK(\"x\")", "B, có phẩy");
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'FAILED', failure_reason = '-Vắng\nnhà', "
                + "status_updated_at = '2026-12-01 03:30:00', recipient_phone = '+84905123456' WHERE plan_id = ? AND label = 'B, có phẩy'", published);
        long draft = createPlan("2026-12-01", "[{\"label\":\"Nháp\",\"lat\":16.06,\"lon\":108.21}]", "");
        long cancelled = createPlan("2026-12-01", "[{\"label\":\"Huỷ\",\"lat\":16.06,\"lon\":108.21}]", "");
        jdbcTemplate.update("UPDATE route_plans SET status = 'CANCELLED' WHERE id = ?", cancelled);

        byte[] content = mockMvc.perform(get(REPORTS + "/stops.csv?from=2026-12-01&to=2026-12-01")
                        .header(HttpHeaders.AUTHORIZATION, store.dispatcher().bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/csv")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("pathwise-20261201-20261201.csv")))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(content).startsWith(0xEF, 0xBB, 0xBF);
        String csv = new String(content, StandardCharsets.UTF_8).substring(1);
        List<String> lines = List.of(csv.split("\r\n"));
        assertThat(lines.getFirst()).startsWith("Ngày,Kế hoạch,Trạng thái kế hoạch,Tài xế,Xe,Thứ tự,Nhãn / mã đơn");
        assertThat(lines).hasSize(4);
        assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\")\"");
        assertThat(csv).contains("\"B, có phẩy\"");
        assertThat(csv).contains("\"Xe \"\"Một\"\"\"");
        assertThat(csv).contains("0905 123 456");
        assertThat(csv).contains("\"'-Vắng\nnhà\"");
        assertThat(csv).contains("Không giao được");
        assertThat(csv).contains("01/12/2026 10:30");
        assertThat(csv).contains("Nháp").doesNotContain("Huỷ,");
        assertThat(draft).isPositive();
    }

    @Test
    void reports_whenOtherStoreOrDriverOrAdminOrLocked_isolateAndAllowReading() throws Exception {
        publishedPlan("2026-12-01", vehicle("Xe 1"), "A");
        TestStore other = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, other.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Other\",\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());

        summary(other.owner(), "?from=2026-12-01")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.plans.total").value(0))
                .andExpect(jsonPath("$.data.stops.total").value(0));
        String csv = mockMvc.perform(get(REPORTS + "/stops.csv?from=2026-12-01").header(HttpHeaders.AUTHORIZATION, other.owner().bearer()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csv.split("\r\n")).hasSize(1);
        summary(store.driver(), "").andExpect(status().isForbidden());
        summary(users.createAdmin(), "").andExpect(status().isForbidden());

        jdbcTemplate.update("UPDATE stores SET status = 'READ_ONLY', lock_reason = 'Kiểm thử', locked_at = NOW(6) WHERE id = ?",
                store.id());
        summary(store.owner(), "?from=2026-12-01").andExpect(status().isOk()).andExpect(jsonPath("$.data.stops.total").value(1));
        mockMvc.perform(get(REPORTS + "/stops.csv?from=2026-12-01").header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isOk());
    }

    private ResultActions summary(TestUser caller, String query) throws Exception {
        return mockMvc.perform(get(REPORTS + "/summary" + query).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private long vehicle(String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":%s,\"capacity\":10,\"capacityUnit\":\"BOX\"}".formatted(json(name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    /**
     * A plan of one route with these stops, optimized and published.
     */
    private long publishedPlan(String day, long vehicle, String... labels) throws Exception {
        StringBuilder stops = new StringBuilder("[");
        for (int i = 0; i < labels.length; i++) {
            if (i > 0) stops.append(',');
            stops.append("{\"label\":").append(json(labels[i])).append(",\"lat\":").append(16.06 + i * 0.001).append(",\"lon\":108.21}");
        }
        stops.append(']');
        long planId = createPlan(day, stops.toString(),
                "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(store.driver().id(), vehicle));
        mockMvc.perform(post(PLANS + "/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        long version = jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
        mockMvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andExpect(status().isOk());
        return planId;
    }

    private long createPlan(String day, String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(day, stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private static String json(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
