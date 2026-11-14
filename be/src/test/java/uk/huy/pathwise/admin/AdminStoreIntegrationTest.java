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
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T55 acceptance (FR-A11): the ADMIN sees every store with correct totals and no store content (P-59); store members
 * get 403.
 */
@AutoConfigureMockMvc
class AdminStoreIntegrationTest extends AbstractIntegrationTest {
    private static final String STORES = "/api/v1/admin/stores";
    private static final String THREE_STOPS = "[{\"label\":\"ĐH-1\",\"recipientName\":\"Chị Lan\",\"recipientPhone\":\"0905123456\","
            + "\"lat\":16.06,\"lon\":108.21},{\"label\":\"ĐH-2\",\"lat\":16.07,\"lon\":108.22},{\"label\":\"ĐH-3\",\"lat\":16.08,\"lon\":108.23}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;
    private TestUser admin;
    private String run;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        admin = users.createAdmin();
        run = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void getStore_whenStoreWorked_returnTotalsLast30DaysAndMembersWithoutContent() throws Exception {
        TestStore store = store("Tiệm " + run);
        long vehicle = vehicle(store, "Xe 1");
        long retired = vehicle(store, "Xe cũ");
        jdbcTemplate.update("UPDATE vehicles SET active = FALSE WHERE id = ?", retired);
        long planId = publishedPlan(store, vehicle);
        long routeId = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ?", Long.class, planId);
        List<Long> stops = jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence",
                Long.class, routeId);
        report(store, routeId, stops.get(0), "{\"status\":\"DELIVERED\"}");
        report(store, routeId, stops.get(1), "{\"status\":\"DELIVERED\"}");
        report(store, routeId, stops.get(2), "{\"status\":\"FAILED\",\"failureReason\":\"Vắng nhà\"}");
        // A second plan, created 40 days ago: counted in the totals, not in the last 30 days.
        long oldPlan = draftPlan(store, vehicle);
        jdbcTemplate.update("UPDATE route_plans SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(40 * 86400L)), oldPlan);

        String body = asAdmin(get(STORES + "/" + store.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tiệm " + run))
                .andExpect(jsonPath("$.data.ownerEmail").value(store.owner().email()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.lockReason").value(nullValue()))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.memberCounts.owners").value(1))
                .andExpect(jsonPath("$.data.memberCounts.dispatchers").value(1))
                .andExpect(jsonPath("$.data.memberCounts.drivers").value(1))
                .andExpect(jsonPath("$.data.vehicles").value(1))
                .andExpect(jsonPath("$.data.plans").value(2))
                .andExpect(jsonPath("$.data.last30Days.plansCreated").value(1))
                .andExpect(jsonPath("$.data.last30Days.plansPublished").value(1))
                .andExpect(jsonPath("$.data.last30Days.plansCompleted").value(1))
                .andExpect(jsonPath("$.data.last30Days.stopsDelivered").value(2))
                .andExpect(jsonPath("$.data.last30Days.stopsFailed").value(1))
                .andExpect(jsonPath("$.data.last30Days.stopsSkipped").value(0))
                .andExpect(jsonPath("$.data.last30Days.deliveredRate").value(0.6667))
                .andExpect(jsonPath("$.data.members.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        DocumentContext json = JsonPath.parse(body);
        assertThat(json.<List<String>>read("$.data.members[*].email"))
                .containsExactly(store.owner().email(), store.dispatcher().email(), store.driver().email());
        assertThat(json.<List<String>>read("$.data.members[*].role")).containsExactly("OWNER", "DISPATCHER", "DRIVER");
        // The latest of the plans' changes and the drivers' reports.
        // Formatted by MariaDB: a JDBC Timestamp would be shifted by the JVM time zone.
        Instant expected = Instant.parse(jdbcTemplate.queryForObject("SELECT DATE_FORMAT(GREATEST("
                + "(SELECT MAX(updated_at) FROM route_plans WHERE store_id = ?), "
                + "(SELECT MAX(status_updated_at) FROM route_plan_stops WHERE store_id = ?)), '%Y-%m-%dT%H:%i:%s.%fZ')",
                String.class, store.id(), store.id()));
        assertThat(Instant.parse(json.read("$.data.lastActivityAt"))).isEqualTo(expected);
        // No content of the store: depot, stops, recipients, phone numbers, names.
        assertThat(body).doesNotContain("depot", "Kho", "ĐH-", "recipient", "Chị Lan", "0905", "phone", "fullName", "lat");
    }

    @Test
    void listStores_whenSearchedFilteredAndSorted_returnMatchingStoresWithTotals() throws Exception {
        TestStore a = store("Tiệm " + run + " A");
        TestStore b = store("Tiệm " + run + " B");
        vehicle(a, "Xe 1");
        jdbcTemplate.update("UPDATE stores SET status = 'READ_ONLY', lock_reason = 'Nợ phí', locked_at = ? WHERE id = ?",
                Timestamp.from(Instant.now()), b.id());

        String body = asAdmin(get(STORES).param("q", run).param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.items[0].id").value(b.id()))
                .andExpect(jsonPath("$.data.items[0].status").value("READ_ONLY"))
                .andExpect(jsonPath("$.data.items[1].id").value(a.id()))
                .andExpect(jsonPath("$.data.items[1].ownerEmail").value(a.owner().email()))
                .andExpect(jsonPath("$.data.items[1].memberCounts.drivers").value(1))
                .andExpect(jsonPath("$.data.items[1].vehicles").value(1))
                .andExpect(jsonPath("$.data.items[1].plans").value(0))
                .andExpect(jsonPath("$.data.items[1].lastActivityAt").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("depot", "fullName", "phone");

        // By OWNER email, case-insensitive.
        asAdmin(get(STORES).param("q", a.owner().email().toUpperCase()))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(a.id()));
        // By state.
        asAdmin(get(STORES).param("q", run).param("status", "READ_ONLY"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(b.id()));
        asAdmin(get(STORES + "/" + b.id()))
                .andExpect(jsonPath("$.data.lockReason").value("Nợ phí"));
        // % and _ are matched literally.
        asAdmin(get(STORES).param("q", run + "%"))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        asAdmin(get(STORES).param("status", "LOCKED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        asAdmin(get(STORES).param("sort", "ownerEmail"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
    }

    @Test
    void adminStores_whenStoreMemberOrUnknownStore_forbiddenOrNotFound() throws Exception {
        TestStore store = store("Tiệm " + run);
        for (TestUser member : List.of(store.owner(), store.dispatcher(), store.driver())) {
            mockMvc.perform(get(STORES).header(HttpHeaders.AUTHORIZATION, member.bearer())).andExpect(status().isForbidden());
            mockMvc.perform(get(STORES + "/" + store.id()).header(HttpHeaders.AUTHORIZATION, member.bearer()))
                    .andExpect(status().isForbidden());
        }
        asAdmin(get(STORES + "/999999999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listUsers_whenFiltered_returnStoreIdAndStatus() throws Exception {
        TestStore store = store("Tiệm " + run);
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", store.driver().id());

        asAdmin(get("/api/v1/admin/users").param("storeId", String.valueOf(store.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items[0].storeId").value(store.id()))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
        asAdmin(get("/api/v1/admin/users").param("storeId", String.valueOf(store.id())).param("status", "DISABLED"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(store.driver().id()))
                .andExpect(jsonPath("$.data.items[0].role").value("DRIVER"));
        asAdmin(get("/api/v1/admin/users").param("storeId", String.valueOf(store.id())).param("role", "DISPATCHER"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(store.dispatcher().id()));
        // An ADMIN has no store.
        asAdmin(get("/api/v1/admin/users").param("role", "ADMIN").param("size", "1"))
                .andExpect(jsonPath("$.data.items[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.data.items[0].storeId").value(nullValue()));
    }

    private ResultActions asAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin.bearer()));
    }

    private TestStore store(String name) throws Exception {
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        return store;
    }

    private long vehicle(TestStore store, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"capacity\":30,\"capacityUnit\":\"BOX\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long draftPlan(TestStore store, long vehicle) throws Exception {
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Giao sáng\",\"planDate\":\"2026-12-01\",\"stops\":%s,\"assignments\":[{\"driverId\":%d,"
                                + "\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                                .formatted(THREE_STOPS, store.driver().id(), vehicle)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long publishedPlan(TestStore store, long vehicle) throws Exception {
        long planId = draftPlan(store, vehicle);
        mockMvc.perform(post("/api/v1/plans/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        long version = jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andExpect(status().isOk());
        return planId;
    }

    private void report(TestStore store, long routeId, long stopId, String body) throws Exception {
        mockMvc.perform(patch("/api/v1/driver/routes/" + routeId + "/stops/" + stopId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, store.driver().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
