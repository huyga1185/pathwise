package uk.huy.pathwise.admin;

import com.jayway.jsonpath.DocumentContext;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T60 acceptance (FR-A16): business numbers are right on known data, by day in Vietnam; technical numbers grow after
 * API calls and a fake failure; ADMIN only; both answer within a second with 100 more stores.
 */
@AutoConfigureMockMvc
class AdminMetricsIntegrationTest extends AbstractIntegrationTest {
    private static final String BUSINESS = "/api/v1/admin/metrics/business";
    private static final String TECHNICAL = "/api/v1/admin/metrics/technical";
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22},"
            + "{\"label\":\"C\",\"lat\":16.08,\"lon\":108.23}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private FailureLog failureLog;

    private TestUsers users;
    private TestUser admin;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        admin = users.createAdmin();
    }

    @Test
    void business_whenDataOnKnownDays_countEachDayInVietnamAndTotals() throws Exception {
        TestStore store = users.createStore();
        TestStore other = users.createStore();
        long planId = completedPlan(store);
        // Move everything to March 2024, which no other test touches. UTC in the database, +7 in Vietnam.
        jdbcTemplate.update("UPDATE stores SET created_at = '2024-03-02 01:00:00' WHERE id = ?", store.id());
        jdbcTemplate.update("UPDATE stores SET created_at = '2024-03-02 18:30:00' WHERE id = ?", other.id()); // 03-03 01:30 VN
        jdbcTemplate.update("UPDATE route_plans SET created_at = '2024-03-02 02:00:00', published_at = '2024-03-02 03:00:00', "
                + "completed_at = '2024-03-03 10:00:00' WHERE id = ?", planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET status_updated_at = '2024-03-03 09:00:00' WHERE plan_id = ?", planId);
        jdbcTemplate.update("UPDATE background_jobs SET created_at = '2024-03-02 02:30:00' WHERE target_id = ? AND type = 'OPTIMIZE_PLAN'", planId);
        jdbcTemplate.update("UPDATE background_jobs SET created_at = '2024-03-02 02:10:00' WHERE target_id = ? AND type <> 'OPTIMIZE_PLAN'", planId);
        jdbcTemplate.update("INSERT INTO background_jobs (store_id, type, target_id, status, attempts, created_by, finished_at, created_at, updated_at) "
                + "VALUES (?, 'OPTIMIZE_PLAN', ?, 'FAILED', 1, ?, '2024-03-04 20:01:00', '2024-03-04 20:00:00', '2024-03-04 20:01:00')",
                store.id(), planId, store.owner().id()); // 03-05 03:00 VN

        DocumentContext json = JsonPath.parse(call(get(BUSINESS).param("from", "2024-03-01").param("to", "2024-03-05"), admin)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.days.length()").value(5))
                .andReturn().getResponse().getContentAsString());

        assertThat(day(json, 0)).containsEntry("date", "2024-03-01").containsEntry("newStores", 0).containsEntry("plansCreated", 0);
        assertThat(day(json, 1)).containsEntry("newStores", 1).containsEntry("plansCreated", 1).containsEntry("plansPublished", 1)
                .containsEntry("optimizations", 1).containsEntry("optimizationsFailed", 0);
        assertThat(day(json, 2)).containsEntry("newStores", 1).containsEntry("plansCompleted", 1)
                .containsEntry("stopsDelivered", 2).containsEntry("stopsFailed", 1).containsEntry("stopsSkipped", 0);
        assertThat(day(json, 4)).containsEntry("optimizations", 1).containsEntry("optimizationsFailed", 1);
        Map<String, Object> totals = json.read("$.data.totals");
        assertThat(totals).containsEntry("newStores", 2).containsEntry("plansCreated", 1).containsEntry("stopsDelivered", 2)
                .containsEntry("deliveredRate", 0.6667).containsEntry("failedRate", 0.3333).containsEntry("skippedRate", 0.0)
                .containsEntry("optimizations", 2).containsEntry("optimizationFailureRate", 0.5);
        Map<String, Object> now = json.read("$.data.now");
        assertThat(((Number) now.get("stores")).longValue()).isGreaterThanOrEqualTo(2);
        assertThat(((Number) now.get("admins")).longValue()).isGreaterThanOrEqualTo(1);
        assertThat(((Number) now.get("activeStores30d")).longValue()).isGreaterThanOrEqualTo(((Number) now.get("activeStores7d")).longValue());

        // Cached about a minute: a change is not seen at once.
        jdbcTemplate.update("UPDATE stores SET created_at = '2024-03-01 05:00:00' WHERE id = ?", other.id());
        call(get(BUSINESS).param("from", "2024-03-01").param("to", "2024-03-05"), admin)
                .andExpect(jsonPath("$.data.computedAt").value((String) json.read("$.data.computedAt")))
                .andExpect(jsonPath("$.data.days[0].newStores").value(0));
    }

    @Test
    void business_whenRangeWrongOrNotAdmin_rejected() throws Exception {
        call(get(BUSINESS).param("from", "2024-03-05").param("to", "2024-03-01"), admin)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("from"))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_DATE_RANGE"));
        call(get(BUSINESS).param("from", "2024-01-01").param("to", "2024-04-02"), admin)
                .andExpect(jsonPath("$.details[0].code").value("DATE_RANGE_TOO_LONG"));
        TestStore store = users.createStore();
        for (TestUser member : List.of(store.owner(), store.dispatcher(), store.driver())) {
            call(get(BUSINESS), member).andExpect(status().isForbidden());
            call(get(TECHNICAL), member).andExpect(status().isForbidden());
        }
        // Default: the last 30 days, today included.
        call(get(BUSINESS), admin)
                .andExpect(jsonPath("$.data.to").value(LocalDate.now(VIETNAM).toString()))
                .andExpect(jsonPath("$.data.days.length()").value(30));
    }

    @Test
    void technical_whenRequestsAndFailures_countersGrow() throws Exception {
        TestStore store = users.createStore();
        DocumentContext before = technical();

        for (int i = 0; i < 3; i++) call(get("/api/v1/vehicles"), store.owner()).andExpect(status().isOk());
        call(get("/api/v1/vehicles/999999999"), store.owner()).andExpect(status().isNotFound());
        failureLog.record(FailureSource.EMAIL, "smtp.send", "send_failed", "", null);

        DocumentContext after = technical();
        assertThat(after.<Number>read("$.data.http.requestsLastHour").longValue())
                .isGreaterThanOrEqualTo(before.<Number>read("$.data.http.requestsLastHour").longValue() + 4);
        assertThat(vehicles(after, "requestsLastHour") - vehicles(before, "requestsLastHour")).isEqualTo(4);
        assertThat(after.<List<Number>>read("$.data.http.groups[?(@.group == 'vehicles')].p95Millis").getFirst().doubleValue()).isPositive();
        assertThat(after.<Number>read("$.data.http.clientErrorRate").doubleValue()).isPositive();
        assertThat(after.<Number>read("$.data.failuresLast24h.email").longValue())
                .isEqualTo(before.<Number>read("$.data.failuresLast24h.email").longValue() + 1);
        assertThat(after.<Number>read("$.data.jvm.heapUsedBytes").longValue()).isPositive();
        assertThat(after.<Number>read("$.data.database.sizeBytes").longValue()).isPositive();
        assertThat(after.<List<Object>>read("$.data.database.largestTables")).hasSizeBetween(1, 5);
        assertThat(after.<Number>read("$.data.database.maxConnections").intValue()).isPositive();
        assertThat(after.<Number>read("$.data.uptimeSeconds").longValue()).isNotNegative();
    }

    @Test
    void metrics_whenHundredMoreStores_answerWithinOneSecond() throws Exception {
        String run = UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.batchUpdate("INSERT INTO stores (name, timezone, created_at, updated_at) VALUES (?, 'Asia/Ho_Chi_Minh', "
                + "UTC_TIMESTAMP(6) - INTERVAL ? DAY, UTC_TIMESTAMP(6))", java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> new Object[]{"Tiệm " + run + " " + i, i % 90}).toList());
        // A range no other call asked: not cached.
        LocalDate to = LocalDate.now(VIETNAM).minusDays(1);

        long start = System.nanoTime();
        call(get(BUSINESS).param("from", to.minusDays(91).toString()).param("to", to.toString()), admin)
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.days.length()").value(92));
        long business = (System.nanoTime() - start) / 1_000_000;
        start = System.nanoTime();
        call(get(TECHNICAL), admin).andExpect(status().isOk());
        long technical = (System.nanoTime() - start) / 1_000_000;

        assertThat(business).as("business, ms").isLessThan(1000);
        assertThat(technical).as("technical, ms").isLessThan(1000);
    }

    private DocumentContext technical() throws Exception {
        return JsonPath.parse(call(get(TECHNICAL), admin).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private static long vehicles(DocumentContext json, String field) {
        List<Number> values = json.read("$.data.http.groups[?(@.group == 'vehicles')]." + field);
        return values.isEmpty() ? 0 : values.getFirst().longValue();
    }

    private static Map<String, Object> day(DocumentContext json, int index) {
        return json.read("$.data.days[" + index + "]");
    }

    private ResultActions call(MockHttpServletRequestBuilder request, TestUser caller) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, TestUser caller, String body) throws Exception {
        return call(request.contentType(MediaType.APPLICATION_JSON).content(body), caller);
    }

    /**
     * A plan published and finished: two stops delivered, one failed.
     */
    private long completedPlan(TestStore store) throws Exception {
        call(put("/api/v1/store"), store.owner(), "{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}").andExpect(status().isOk());
        String vehicle = call(post("/api/v1/vehicles"), store.owner(), "{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andReturn().getResponse().getContentAsString();
        long vehicleId = ((Number) JsonPath.read(vehicle, "$.data.id")).longValue();
        String body = call(post("/api/v1/plans"), store.owner(), ("{\"name\":\"Giao\",\"planDate\":\"2026-12-04\",\"stops\":%s,"
                + "\"assignments\":[{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                .formatted(STOPS, store.driver().id(), vehicleId)).andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        call(post("/api/v1/plans/" + planId + "/optimize"), store.owner(), "{\"version\":0}")
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        long version = jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
        call(post("/api/v1/plans/" + planId + "/publish"), store.owner(), "{\"version\":" + version + "}").andExpect(status().isOk());
        long routeId = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ?", Long.class, planId);
        List<Long> stops = jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence", Long.class, routeId);
        String[] results = {"{\"status\":\"DELIVERED\"}", "{\"status\":\"DELIVERED\"}", "{\"status\":\"FAILED\",\"failureReason\":\"Vắng nhà\"}"};
        for (int i = 0; i < 3; i++) {
            call(patch("/api/v1/driver/routes/" + routeId + "/stops/" + stops.get(i) + "/status"), store.driver(), results[i])
                    .andExpect(status().isOk());
        }
        return planId;
    }
}
