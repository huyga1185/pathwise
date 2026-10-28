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
import uk.huy.pathwise.support.FakeMatrixProvider;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T25 acceptance: {@code POST /plans/{id}/optimize} (FR-P04, ARCHITECTURE.md §8.3) with the travel matrix of
 * {@link FakeMatrixProvider} and the real OR-Tools solver (1 s in tests). Run once per job mode by the subclasses;
 * every test waits for the plan to be IDLE, so it holds for both.
 */
@AutoConfigureMockMvc
abstract class OptimizePlanIntegrationTest extends AbstractIntegrationTest {
    static final String PLANS = "/api/v1/plans";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    FakeEmailSender emailSender;

    @Autowired
    FakeMatrixProvider matrix;

    TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void optimize_whenPlanReady_writeRoutesWithEtasAndIdleAgain() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), """
                [{"label":"A","lat":16.06,"lon":108.21,"demands":{"BOX":3},"timeWindow":{"start":"09:00","end":"10:00"}},
                 {"label":"B","lat":16.07,"lon":108.22,"demands":{"BOX":2},"serviceTimeSeconds":300},
                 {"label":"C","lat":16.055,"lon":108.205}]""", assignment(store.driver().id(), vehicle));

        String job = optimize(store.dispatcher(), planId, 0)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.type").value("OPTIMIZE_PLAN"))
                .andExpect(jsonPath("$.data.planId").value(planId))
                .andReturn().getResponse().getContentAsString();

        awaitIdle(planId);
        String body = getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.activeJob").value(nullValue()))
                // One for the request, one for the result (P-30).
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.optimizedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(0)))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(3)))
                .andExpect(jsonPath("$.data.routes[0].stops[*].sequence", contains(1, 2, 3)))
                .andExpect(jsonPath("$.data.routes[0].load").value(5))
                .andExpect(jsonPath("$.data.routes[0].plannedDistanceMeters").isNumber())
                .andReturn().getResponse().getContentAsString();
        int routeDistance = JsonPath.read(body, "$.data.routes[0].plannedDistanceMeters");
        int totalDistance = JsonPath.read(body, "$.data.totalDistanceMeters");
        assertThat(routeDistance).isPositive().isEqualTo(totalDistance);
        // 2026-11-02 in Asia/Ho_Chi_Minh (UTC+7): shift from 01:00Z, window of A from 02:00Z to 03:00Z.
        List<String> labels = JsonPath.read(body, "$.data.routes[0].stops[*].label");
        List<String> arrivals = JsonPath.read(body, "$.data.routes[0].stops[*].plannedArrival");
        Instant arrivalOfA = Instant.parse(arrivals.get(labels.indexOf("A")));
        assertThat(arrivalOfA).isBetween(Instant.parse("2026-11-02T02:00:00Z"), Instant.parse("2026-11-02T03:00:00Z"));
        assertThat(Instant.parse(JsonPath.read(body, "$.data.routes[0].plannedStart")))
                .isAfterOrEqualTo(Instant.parse("2026-11-02T01:00:00Z"));
        getJob(store.owner(), id(job)).andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    @Test
    void optimize_whenCapacityTooSmall_dropStopAsInfeasible() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe nhỏ", 5, "BOX");
        long planId = createPlan(store.owner(), """
                [{"label":"A","lat":16.06,"lon":108.21,"demands":{"BOX":4}},
                 {"label":"B","lat":16.07,"lon":108.22,"demands":{"BOX":4}}]""", assignment(store.driver().id(), vehicle));

        optimize(store.owner(), planId, 0).andExpect(status().isAccepted());

        awaitIdle(planId);
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(1)))
                .andExpect(jsonPath("$.data.routes[0].load").value(4))
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(1)))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("INFEASIBLE"));
    }

    @Test
    void optimize_whenVehicleCountsOrders_countEveryStopAsOne() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe máy", 1, "ORDER");
        long planId = createPlan(store.owner(), """
                [{"label":"A","lat":16.06,"lon":108.21},{"label":"B","lat":16.07,"lon":108.22,"demands":{"KG":50}}]""",
                assignment(store.driver().id(), vehicle));

        optimize(store.owner(), planId, 0).andExpect(status().isAccepted());

        awaitIdle(planId);
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(1)))
                .andExpect(jsonPath("$.data.routes[0].load").value(1))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("INFEASIBLE"));
    }

    @Test
    void optimize_whenUnusedVehicle_routeHasNoStopsAndDoesNotDrive() throws Exception {
        TestStore store = storeWithDepot();
        long first = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long second = createVehicle(store.owner(), "Xe 2", 10, "BOX");
        TestUser otherDriver = users.addMember(store.id(), uk.huy.pathwise.shared.identity.UserRole.DRIVER, false);
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]",
                assignment(store.driver().id(), first) + "," + assignment(otherDriver.id(), second));

        optimize(store.owner(), planId, 0).andExpect(status().isAccepted());

        awaitIdle(planId);
        String body = getPlan(store.owner(), planId).andReturn().getResponse().getContentAsString();
        List<Integer> stopCounts = JsonPath.read(body, "$.data.routes[*].stops.length()");
        assertThat(stopCounts).containsExactlyInAnyOrder(0, 1);
        int unused = stopCounts.indexOf(0);
        assertThat((Integer) JsonPath.read(body, "$.data.routes[" + unused + "].plannedDistanceMeters")).isZero();
        assertThat((Object) JsonPath.read(body, "$.data.routes[" + unused + "].plannedStart")).isNull();
        assertThat((Integer) JsonPath.read(body, "$.data.routes[" + unused + "].load")).isZero();
    }

    @Test
    void optimize_whenNoStopsOrNoAssignments_return422PlanIncomplete() throws Exception {
        TestStore store = storeWithDepot();
        long empty = createPlan(store.owner(), "[]", "");
        long withoutAssignments = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", "");

        optimize(store.owner(), empty, 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_INCOMPLETE"))
                .andExpect(jsonPath("$.details[?(@.field == 'stops')].code").value("STOPS_REQUIRED"))
                .andExpect(jsonPath("$.details[?(@.field == 'assignments')].code").value("ASSIGNMENTS_REQUIRED"));
        optimize(store.owner(), withoutAssignments, 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("assignments"));
        assertThat(processingStatus(empty)).isEqualTo("IDLE");
    }

    @Test
    void optimize_whenStopWithoutCoordinates_return422StopsNotGeocoded() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        // The fake geocoder knows no address, so this stop ends FAILED.
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"address\":\"không có " + System.nanoTime() + "\"}]",
                assignment(store.driver().id(), vehicle));
        awaitIdle(planId);

        optimize(store.owner(), planId, version(planId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("STOPS_NOT_GEOCODED"));
    }

    @Test
    void optimize_whenNotDraftOrStaleVersion_return422Or409() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", assignment(store.driver().id(), vehicle));

        optimize(store.owner(), planId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));
        jdbcTemplate.update("UPDATE route_plans SET status = 'PUBLISHED' WHERE id = ?", planId);
        optimize(store.owner(), planId, 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
    }

    @Test
    void optimize_whenDriverDisabledSinceSaved_return422InvalidAssignmentOnThatField() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", assignment(store.driver().id(), vehicle));
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", store.driver().id());
        jdbcTemplate.update("UPDATE vehicles SET active = FALSE WHERE id = ?", vehicle);

        optimize(store.owner(), planId, 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"))
                .andExpect(jsonPath("$.details[*].field", contains("assignments[0].driverId", "assignments[0].vehicleId")));
    }

    @Test
    void optimize_whenMatrixProviderFails_failJobAndKeepPreviousResult() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", assignment(store.driver().id(), vehicle));
        optimize(store.owner(), planId, 0).andExpect(status().isAccepted());
        awaitIdle(planId);
        String before = JsonPath.read(getPlan(store.owner(), planId).andReturn().getResponse().getContentAsString(), "$.data.optimizedAt");
        matrix.failNext();

        String job = optimize(store.owner(), planId, version(planId)).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        awaitIdle(planId);
        getJob(store.owner(), id(job))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorCode").value("ROUTING_PROVIDER_ERROR"));
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.optimizedAt").value(before))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(1)))
                .andExpect(jsonPath("$.data.activeJob").value(nullValue()));
    }

    @Test
    void optimize_whenPlanChangedWhileSolving_failJobAndWriteNothing() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", assignment(store.driver().id(), vehicle));
        // Another writer moves the version after the job has read the plan.
        matrix.onNextCall(() -> jdbcTemplate.update("UPDATE route_plans SET version = version + 1 WHERE id = ?", planId));

        String job = optimize(store.owner(), planId, 0).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        awaitIdle(planId);
        getJob(store.owner(), id(job))
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.errorCode").value("PLAN_MODIFIED"));
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.optimizedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("NOT_OPTIMIZED"));
    }

    @Test
    void optimize_whenPlanOfAnotherStoreOrDriver_return404Or403() throws Exception {
        TestStore storeA = storeWithDepot();
        TestStore storeB = storeWithDepot();
        long planId = createPlan(storeA.owner(), "[]", "");

        optimize(storeB.owner(), planId, 0).andExpect(status().isNotFound());
        optimize(storeA.driver(), planId, 0).andExpect(status().isForbidden());
    }

    TestStore storeWithDepot() throws Exception {
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        return store;
    }

    long createVehicle(TestUser caller, String name, int capacity, String unit) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":%d,\"capacityUnit\":\"%s\"}".formatted(name, capacity, unit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    long createPlan(TestUser caller, String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, caller.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"planDate\":\"2026-11-02\",\"stops\":%s,\"assignments\":[%s]}".formatted(stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    ResultActions optimize(TestUser caller, long planId, long version) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"));
    }

    ResultActions getPlan(TestUser caller, long planId) throws Exception {
        return mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    ResultActions getJob(TestUser caller, long jobId) throws Exception {
        return mockMvc.perform(get("/api/v1/jobs/" + jobId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    /**
     * Until the plan is IDLE and its jobs have finished. The handler releases the plan before the executor records
     * the final job status, so an async job may still read RUNNING for a moment after the plan is IDLE (F-45).
     */
    void awaitIdle(long planId) {
        await().atMost(Duration.ofSeconds(30)).until(() -> processingStatus(planId).equals("IDLE") && unfinishedJobs(planId) == 0);
    }

    int unfinishedJobs(long planId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM background_jobs WHERE target_id = ? AND status IN ('QUEUED', 'RUNNING')",
                Integer.class, planId);
    }

    String processingStatus(long planId) {
        return jdbcTemplate.queryForObject("SELECT processing_status FROM route_plans WHERE id = ?", String.class, planId);
    }

    long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }

    static long id(String body) {
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }
}
