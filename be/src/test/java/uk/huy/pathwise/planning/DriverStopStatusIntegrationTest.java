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
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T31 acceptance: the driver reports stops of their route and the plan follows (FR-D03, BR-10, BR-11, API_SPEC §8).
 */
@AutoConfigureMockMvc
class DriverStopStatusIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTES = "/api/v1/driver/routes";
    private static final String DAY = "2026-11-02";
    private static final String TWO_STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22}]";
    private static final String DELIVERED = "{\"status\":\"DELIVERED\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void updateStopStatus_whenFirstStopDelivered_planInProgressAndVersionUnchanged() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long versionBefore = version(planId);
        long routeId = routeOf(planId, store.driver());
        long[] stops = stopsOf(routeId);

        updateStatus(store.driver(), routeId, stops[0], DELIVERED)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stop.id").value(stops[0]))
                .andExpect(jsonPath("$.data.stop.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.stop.failureReason").isEmpty())
                .andExpect(jsonPath("$.data.routeStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.planStatus").value("IN_PROGRESS"));

        assertThat(version(planId)).isEqualTo(versionBefore);
        Map<String, Object> stop = jdbcTemplate.queryForMap(
                "SELECT status, status_updated_by, status_updated_at FROM route_plan_stops WHERE id = ?", stops[0]);
        assertThat(stop.get("status")).isEqualTo("DELIVERED");
        assertThat(((Number) stop.get("status_updated_by")).longValue()).isEqualTo(store.driver().id());
        assertThat(stop.get("status_updated_at")).isNotNull();
        dispatcherView(store, planId)
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.version").value(versionBefore))
                .andExpect(jsonPath("$.data.routes[0].stops[0].status").value("DELIVERED"));
    }

    @Test
    void updateStopStatus_whenLastStopFinishes_planCompleted() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long versionBefore = version(planId);
        long routeId = routeOf(planId, store.driver());
        long[] stops = stopsOf(routeId);

        updateStatus(store.driver(), routeId, stops[0], "{\"status\":\"SKIPPED\",\"failureReason\":\"bỏ qua\"}")
                .andExpect(jsonPath("$.data.stop.failureReason").isEmpty());
        updateStatus(store.driver(), routeId, stops[1], "{\"status\":\"FAILED\",\"failureReason\":\"  Vắng nhà \"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stop.status").value("FAILED"))
                .andExpect(jsonPath("$.data.stop.failureReason").value("Vắng nhà"))
                .andExpect(jsonPath("$.data.routeStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.planStatus").value("COMPLETED"));

        assertThat(version(planId)).isEqualTo(versionBefore);
        assertThat(jdbcTemplate.queryForObject("SELECT completed_at FROM route_plans WHERE id = ?", Object.class, planId)).isNotNull();
        getRoute(store.driver(), routeId).andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void updateStopStatus_whenOnlyStopFinishes_planGoesStraightToCompleted() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]",
                assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());

        updateStatus(store.driver(), routeId, stopsOf(routeId)[0], DELIVERED)
                .andExpect(jsonPath("$.data.planStatus").value("COMPLETED"));
        assertThat(planStatus(planId)).isEqualTo("COMPLETED");
    }

    @Test
    void updateStopStatus_whenOneOfTwoRoutesCompleted_routeCompletedButPlanInProgress() throws Exception {
        TestStore store = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, oneBoxStops(), assignment(store.driver().id(), vehicle(store, "Xe 1", 1))
                + "," + assignment(colleague.id(), vehicle(store, "Xe 2", 1)));
        long routeId = routeOf(planId, store.driver());

        updateStatus(store.driver(), routeId, stopsOf(routeId)[0], DELIVERED)
                .andExpect(jsonPath("$.data.routeStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.planStatus").value("IN_PROGRESS"));
    }

    @Test
    void updateStopStatus_whenFailedWithoutReason_return422() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        long stop = stopsOf(routeId)[0];

        for (String body : List.of("{\"status\":\"FAILED\"}", "{\"status\":\"FAILED\",\"failureReason\":\"   \"}")) {
            updateStatus(store.driver(), routeId, stop, body)
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.code").value("FAILURE_REASON_REQUIRED"));
        }
        assertThat(stopStatus(stop)).isEqualTo("PENDING");
        assertThat(planStatus(planId)).isEqualTo("PUBLISHED");
    }

    @Test
    void updateStopStatus_whenSameStatusSentAgain_return200AndChangeNothing() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]",
                assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        long stop = stopsOf(routeId)[0];
        updateStatus(store.driver(), routeId, stop, "{\"status\":\"FAILED\",\"failureReason\":\"Vắng nhà\"}");
        Map<String, Object> before = jdbcTemplate.queryForMap("SELECT * FROM route_plan_stops WHERE id = ?", stop);
        Map<String, Object> planBefore = jdbcTemplate.queryForMap("SELECT * FROM route_plans WHERE id = ?", planId);

        // The plan has completed meanwhile: a retry of the last update still succeeds.
        updateStatus(store.driver(), routeId, stop, "{\"status\":\"FAILED\",\"failureReason\":\"Lý do khác\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stop.failureReason").value("Vắng nhà"))
                .andExpect(jsonPath("$.data.routeStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.planStatus").value("COMPLETED"));

        assertThat(jdbcTemplate.queryForMap("SELECT * FROM route_plan_stops WHERE id = ?", stop)).isEqualTo(before);
        assertThat(jdbcTemplate.queryForMap("SELECT * FROM route_plans WHERE id = ?", planId)).isEqualTo(planBefore);
    }

    @Test
    void updateStopStatus_whenStopFinishedOrTargetPending_return422InvalidTransition() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        long[] stops = stopsOf(routeId);
        updateStatus(store.driver(), routeId, stops[0], DELIVERED);

        updateStatus(store.driver(), routeId, stops[0], "{\"status\":\"SKIPPED\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_STOP_TRANSITION"));
        updateStatus(store.driver(), routeId, stops[0], "{\"status\":\"PENDING\"}")
                .andExpect(jsonPath("$.code").value("INVALID_STOP_TRANSITION"));
        updateStatus(store.driver(), routeId, stops[1], "{\"status\":\"PENDING\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_STOP_TRANSITION"));
        assertThat(stopStatus(stops[0])).isEqualTo("DELIVERED");
    }

    @Test
    void updateStopStatus_whenPlanCancelled_return422PlanNotActive() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        mockMvc.perform(post("/api/v1/plans/" + planId + "/cancel").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());

        updateStatus(store.driver(), routeId, stopsOf(routeId)[0], DELIVERED)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
        assertThat(planStatus(planId)).isEqualTo("CANCELLED");
    }

    @Test
    void updateStopStatus_whenPlanNeverPublished_return404() throws Exception {
        TestStore store = storeWithDepot();
        long planId = optimizedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        long stop = stopsOf(routeId)[0];

        updateStatus(store.driver(), routeId, stop, DELIVERED).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/plans/" + planId + "/cancel").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());
        updateStatus(store.driver(), routeId, stop, DELIVERED)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void updateStopStatus_whenRouteOrStopNotCallers_return404() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, oneBoxStops(), assignment(store.driver().id(), vehicle(store, "Xe 1", 1))
                + "," + assignment(colleague.id(), vehicle(store, "Xe 2", 1)));
        long routeId = routeOf(planId, store.driver());
        long stop = stopsOf(routeId)[0];
        long colleagueStop = stopsOf(routeOf(planId, colleague))[0];

        updateStatus(colleague, routeId, stop, DELIVERED)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        updateStatus(otherStore.driver(), routeId, stop, DELIVERED).andExpect(status().isNotFound());
        updateStatus(store.driver(), routeId, colleagueStop, DELIVERED).andExpect(status().isNotFound());
        updateStatus(store.driver(), routeId, 999_999, DELIVERED).andExpect(status().isNotFound());
        assertThat(stopStatus(stop)).isEqualTo("PENDING");
        assertThat(stopStatus(colleagueStop)).isEqualTo("PENDING");
    }

    @Test
    void updateStopStatus_whenDispatcherWithoutCanDrive_return403() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());

        updateStatus(store.dispatcher(), routeId, stopsOf(routeId)[0], DELIVERED)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void updateStopStatus_whenBodyInvalid_return400() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, TWO_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = routeOf(planId, store.driver());
        long stop = stopsOf(routeId)[0];

        updateStatus(store.driver(), routeId, stop, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("status"))
                .andExpect(jsonPath("$.details[0].code").value("STOP_STATUS_REQUIRED"));
        updateStatus(store.driver(), routeId, stop, "{\"status\":\"LOST\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        updateStatus(store.driver(), routeId, stop, "{\"status\":\"FAILED\",\"failureReason\":\"" + "x".repeat(256) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("failureReason"))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_FAILURE_REASON_LENGTH"));
    }

    @Test
    void updateStopStatus_whenTwoDriversFinishLastStopsAtOnce_planCompleted() throws Exception {
        TestStore store = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, oneBoxStops(), assignment(store.driver().id(), vehicle(store, "Xe 1", 1))
                + "," + assignment(colleague.id(), vehicle(store, "Xe 2", 1)));
        long route = routeOf(planId, store.driver());
        long colleagueRoute = routeOf(planId, colleague);
        long stop = stopsOf(route)[0];
        long colleagueStop = stopsOf(colleagueRoute)[0];
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> {
                start.await();
                return updateStatus(store.driver(), route, stop, DELIVERED).andReturn().getResponse().getStatus();
            });
            Future<Integer> b = pool.submit(() -> {
                start.await();
                return updateStatus(colleague, colleagueRoute, colleagueStop, DELIVERED).andReturn().getResponse().getStatus();
            });

            assertThat(List.of(a.get(), b.get())).containsExactly(200, 200);
        } finally {
            pool.shutdownNow();
        }
        assertThat(planStatus(planId)).isEqualTo("COMPLETED");
    }

    /**
     * Two stops of one box each: with two vehicles of one box, each driver gets one stop.
     */
    private static String oneBoxStops() {
        return "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":1}},"
                + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22,\"demands\":{\"BOX\":1}}]";
    }

    private TestStore storeWithDepot() throws Exception {
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        return store;
    }

    private long vehicle(TestStore store, String name, int boxes) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":%d,\"capacityUnit\":\"BOX\"}".formatted(name, boxes)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long optimizedPlan(TestStore store, String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Giao sáng\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(DAY, stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        mockMvc.perform(post("/api/v1/plans/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        return planId;
    }

    private long publishedPlan(TestStore store, String stops, String assignments) throws Exception {
        long planId = optimizedPlan(store, stops, assignments);
        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());
        return planId;
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    private long routeOf(long planId, TestUser driver) {
        return jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ? AND driver_id = ?",
                Long.class, planId, driver.id());
    }

    /**
     * @return the stops of the route, by sequence
     */
    private long[] stopsOf(long routeId) {
        return jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence", Long.class, routeId)
                .stream().mapToLong(Long::longValue).toArray();
    }

    private ResultActions updateStatus(TestUser caller, long routeId, long stopId, String body) throws Exception {
        return mockMvc.perform(patch(ROUTES + "/" + routeId + "/stops/" + stopId + "/status")
                .header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions getRoute(TestUser caller, long routeId) throws Exception {
        return mockMvc.perform(get(ROUTES + "/" + routeId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private ResultActions dispatcherView(TestStore store, long planId) throws Exception {
        return mockMvc.perform(get("/api/v1/plans/" + planId).header(HttpHeaders.AUTHORIZATION, store.dispatcher().bearer()));
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }

    private String planStatus(long planId) {
        return jdbcTemplate.queryForObject("SELECT status FROM route_plans WHERE id = ?", String.class, planId);
    }

    private String stopStatus(long stopId) {
        return jdbcTemplate.queryForObject("SELECT status FROM route_plan_stops WHERE id = ?", String.class, stopId);
    }
}
