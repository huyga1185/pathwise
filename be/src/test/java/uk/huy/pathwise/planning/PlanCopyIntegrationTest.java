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
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.FakeGeocodingProvider;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T62 acceptance: {@code POST /plans/{id}/copy} (P-67, P-68).
 */
@AutoConfigureMockMvc
class PlanCopyIntegrationTest extends AbstractIntegrationTest {
    private static final String PLANS = "/api/v1/plans";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private FakeGeocodingProvider geocoding;

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
    void copy_whenAll_copyStopsWithTheirLocationAndAssignmentsIntoANewDraft() throws Exception {
        String found = "1 Lê Duẩn copy " + System.nanoTime();
        String missing = "Không có copy " + System.nanoTime();
        geocoding.resolve(found, 16.07, 108.22);
        long vehicle = vehicle("Xe 1");
        long planId = createPlan("[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":5},"
                        + "\"timeWindow\":{\"start\":\"09:00\",\"end\":\"11:00\"},\"recipientPhone\":\"0905123456\",\"note\":\"N\"},"
                        + "{\"label\":\"B\",\"address\":\"" + found + "\"},{\"label\":\"C\",\"address\":\"" + missing + "\"}]",
                assignment(store.driver().id(), vehicle));
        jdbcTemplate.update("UPDATE route_plans SET status = 'COMPLETED' WHERE id = ?", planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'DELIVERED' WHERE plan_id = ? AND label = 'A'", planId);
        int searches = geocoding.calls(found);

        String body = copy(store.dispatcher(), planId, "{\"planDate\":\"2026-12-02\",\"stops\":\"ALL\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plan.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.plan.processingStatus").value("IDLE"))
                .andExpect(jsonPath("$.data.plan.version").value(0))
                .andExpect(jsonPath("$.data.plan.planDate").value("2026-12-02"))
                .andExpect(jsonPath("$.data.plan.name").value("P (bản sao)"))
                .andExpect(jsonPath("$.data.plan.optimizedAt").doesNotExist())
                .andExpect(jsonPath("$.data.plan.routes", hasSize(1)))
                .andExpect(jsonPath("$.data.plan.routes[0].driver.id").value(store.driver().id()))
                .andExpect(jsonPath("$.data.plan.routes[0].shiftStart").value("08:00"))
                .andExpect(jsonPath("$.data.plan.unassignedStops", hasSize(3)))
                .andExpect(jsonPath("$.data.skippedAssignments", hasSize(0)))
                .andReturn().getResponse().getContentAsString();

        long copyId = ((Number) JsonPath.read(body, "$.data.plan.id")).longValue();
        List<Map<String, Object>> stops = jdbcTemplate.queryForList("SELECT label, status, geocode_status, geocode_error, lat, "
                + "geocoded_display_name, recipient_phone, tw_start, note FROM route_plan_stops WHERE plan_id = ? ORDER BY id", copyId);
        assertThat(stops).extracting(row -> row.get("label")).containsExactly("A", "B", "C");
        assertThat(stops).extracting(row -> row.get("status")).containsOnly("PENDING");
        assertThat(stops).extracting(row -> row.get("geocode_status")).containsExactly("PROVIDED", "RESOLVED", "FAILED");
        assertThat(stops.get(1).get("geocoded_display_name")).isEqualTo(found + ", Việt Nam");
        assertThat(stops.get(2).get("geocode_error")).isEqualTo("NOT_FOUND");
        assertThat(stops.get(0).get("recipient_phone")).isEqualTo("+84905123456");
        assertThat(stops.get(0).get("note")).isEqualTo("N");
        assertThat(jdbcTemplate.queryForObject("SELECT quantity FROM route_plan_stop_demands d JOIN route_plan_stops s "
                + "ON s.id = d.stop_id WHERE s.plan_id = ? AND d.unit = 'BOX'", Integer.class, copyId)).isEqualTo(5);
        assertThat(geocoding.calls(found)).isEqualTo(searches);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM route_plans WHERE id = ?", String.class, planId))
                .isEqualTo("COMPLETED");
    }

    @Test
    void copy_whenNotDelivered_onlyFailedSkippedAndPendingStops() throws Exception {
        long planId = createPlan("[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"B\",\"lat\":16.06,\"lon\":108.21},"
                + "{\"label\":\"C\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"D\",\"lat\":16.06,\"lon\":108.21}]", "");
        jdbcTemplate.update("UPDATE route_plans SET status = 'CANCELLED' WHERE id = ?", planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'DELIVERED' WHERE plan_id = ? AND label = 'A'", planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'FAILED', failure_reason = 'Vắng' WHERE plan_id = ? AND label = 'B'", planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'SKIPPED' WHERE plan_id = ? AND label = 'C'", planId);

        String body = copy(store.owner(), planId, "{\"planDate\":\"2026-12-03\",\"name\":\"  Giao lại  \",\"stops\":\"NOT_DELIVERED\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plan.name").value("Giao lại"))
                .andReturn().getResponse().getContentAsString();

        long copyId = ((Number) JsonPath.read(body, "$.data.plan.id")).longValue();
        assertThat(jdbcTemplate.queryForList("SELECT label FROM route_plan_stops WHERE plan_id = ? ORDER BY id", String.class, copyId))
                .containsExactly("B", "C", "D");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM route_plan_stops WHERE plan_id = ? AND failure_reason IS NOT NULL",
                Long.class, copyId)).isZero();

        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'DELIVERED' WHERE plan_id = ?", planId);
        copy(store.owner(), planId, "{\"planDate\":\"2026-12-03\",\"stops\":\"NOT_DELIVERED\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_COPY"));
    }

    @Test
    void copy_whenDriverDisabledOrVehicleInactive_skipTheirAssignments() throws Exception {
        TestUser second = users.addMember(store.id(), UserRole.DRIVER, false);
        users.withCanDrive(store.owner(), true);
        long vehicleA = vehicle("Xe A");
        long vehicleB = vehicle("Xe B");
        long vehicleC = vehicle("Xe C");
        long planId = createPlan("[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]",
                assignment(store.driver().id(), vehicleA) + "," + assignment(second.id(), vehicleB) + ","
                        + assignment(store.owner().id(), vehicleC));
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", second.id());
        jdbcTemplate.update("UPDATE vehicles SET active = FALSE WHERE id = ?", vehicleA);
        jdbcTemplate.update("UPDATE users SET can_drive = FALSE WHERE id = ?", store.owner().id());

        copy(store.owner(), planId, "{\"planDate\":\"2026-12-04\",\"stops\":\"ALL\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.plan.routes", hasSize(0)))
                .andExpect(jsonPath("$.data.skippedAssignments", hasSize(3)))
                .andExpect(jsonPath("$.data.skippedAssignments[0].driverId").value(store.driver().id()))
                .andExpect(jsonPath("$.data.skippedAssignments[0].reason").value("VEHICLE_INACTIVE"))
                .andExpect(jsonPath("$.data.skippedAssignments[1].reason").value("DRIVER_UNAVAILABLE"))
                .andExpect(jsonPath("$.data.skippedAssignments[2].reason").value("DRIVER_UNAVAILABLE"));
    }

    @Test
    void copy_whenPendingGeocodingStopOrLongName_geocodeTheCopyAndCutTheName() throws Exception {
        String address = "Chưa tìm copy " + System.nanoTime();
        String longName = "N".repeat(150);
        long planId = createPlan("[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", "");
        jdbcTemplate.update("UPDATE route_plans SET name = ? WHERE id = ?", longName, planId);
        jdbcTemplate.update("UPDATE route_plan_stops SET geocode_status = 'PENDING', lat = NULL, lon = NULL, address_text = ? "
                + "WHERE plan_id = ?", address, planId);
        geocoding.resolve(address, 16.08, 108.23);

        String body = copy(store.owner(), planId, "{\"planDate\":\"2026-12-05\",\"stops\":\"ALL\"}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String name = JsonPath.read(body, "$.data.plan.name");
        assertThat(name).hasSize(150).endsWith(" (bản sao)");
        long copyId = ((Number) JsonPath.read(body, "$.data.plan.id")).longValue();
        assertThat(jdbcTemplate.queryForObject("SELECT geocode_status FROM route_plan_stops WHERE plan_id = ?", String.class, copyId))
                .isEqualTo("RESOLVED");
        assertThat(jdbcTemplate.queryForObject("SELECT processing_status FROM route_plans WHERE id = ?", String.class, copyId))
                .isEqualTo("IDLE");
    }

    @Test
    void copy_whenOtherStoreOrDriverOrLockedOrBadRequest_refuse() throws Exception {
        long planId = createPlan("[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", "");
        TestStore other = users.createStore();
        String ok = "{\"planDate\":\"2026-12-06\",\"stops\":\"ALL\"}";

        copy(other.owner(), planId, ok).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        copy(store.driver(), planId, ok).andExpect(status().isForbidden());
        copy(store.owner(), planId, "{\"stops\":\"ALL\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("planDate"))
                .andExpect(jsonPath("$.details[0].code").value("PLAN_DATE_REQUIRED"));
        copy(store.owner(), planId, "{\"planDate\":\"2026-12-06\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("COPY_SCOPE_REQUIRED"));
        copy(store.owner(), planId, "{\"planDate\":\"2026-12-06\",\"stops\":\"SOME\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        jdbcTemplate.update("UPDATE stores SET status = 'READ_ONLY', lock_reason = 'Kiểm thử', locked_at = NOW(6) WHERE id = ?",
                store.id());
        copy(store.owner(), planId, ok).andExpect(status().is(423)).andExpect(jsonPath("$.code").value("STORE_READ_ONLY"));
    }

    private ResultActions copy(TestUser caller, long planId, String body) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/copy").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long vehicle(String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":10,\"capacityUnit\":\"BOX\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long createPlan(String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"planDate\":\"2026-12-01\",\"stops\":%s,\"assignments\":[%s]}".formatted(stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"12:00\"}".formatted(driverId, vehicleId);
    }
}
