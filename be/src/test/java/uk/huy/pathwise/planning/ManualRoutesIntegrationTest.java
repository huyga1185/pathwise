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
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.FakeMatrixProvider;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T28 acceptance: manual route editing after optimization (FR-P08, D-24, P-58, API_SPEC §7).
 */
@AutoConfigureMockMvc
class ManualRoutesIntegrationTest extends AbstractIntegrationTest {
    private static final Coordinate DEPOT = new Coordinate(16.05, 108.2);
    /** 4 boxes each; vehicles carry 10, so all four on one route are 6 boxes too many. */
    private static final String FOUR_STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":4}},"
            + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22,\"demands\":{\"BOX\":4}},"
            + "{\"label\":\"C\",\"lat\":16.04,\"lon\":108.19,\"demands\":{\"BOX\":4}},"
            + "{\"label\":\"D\",\"lat\":16.045,\"lon\":108.205,\"demands\":{\"BOX\":4}}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private FakeMatrixProvider matrixProvider;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void replaceRoutes_whenAllStopsOnOneRoute_recomputeAndReportCapacityShiftAndDistance() throws Exception {
        TestStore store = storeWithDepot();
        // Each half of the stops fits a 12-minute shift of at most 6 km; all four on one route (about 17 minutes,
        // 10 km at the fake's 10 m/s) do not. The optimizer may leave a stop out: the edit sends all four anyway.
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store, "08:12", 6000));
        List<Long> routes = routeIds(planId);
        List<Long> stops = stopIds(planId);
        long version = version(planId);
        AtomicReference<Boolean> matrixInTransaction = new AtomicReference<>();
        matrixProvider.onNextCall(() -> matrixInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive()));

        String body = replace(store.owner(), planId, version, List.of(stops, List.of()), List.of(), routes)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(version + 1))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(4)))
                .andExpect(jsonPath("$.data.routes[0].load").value(16))
                .andExpect(jsonPath("$.data.routes[1].stops", hasSize(0)))
                .andExpect(jsonPath("$.data.routes[1].plannedDistanceMeters").value(0))
                .andExpect(jsonPath("$.data.routes[1].plannedStart").value((Object) null))
                .andReturn().getResponse().getContentAsString();

        assertThat(matrixInTransaction.get()).isFalse();
        // The order sent, and the distance of exactly that order with the fake's straight lines.
        assertThat(JsonPath.<List<Integer>>read(body, "$.data.routes[0].stops[*].id").stream().map(Integer::longValue).toList())
                .isEqualTo(stops);
        int expected = (int) Math.round(legsMeters(planId, stops));
        int distance = JsonPath.read(body, "$.data.routes[0].plannedDistanceMeters");
        assertThat(distance).isBetween(expected - 4, expected + 4);
        assertThat(JsonPath.<Integer>read(body, "$.data.totalDistanceMeters")).isEqualTo(distance);
        // No time window: the driver leaves at 08:00 (01:00 UTC) and reaches the first stop after its leg at 10 m/s.
        Instant firstArrival = Instant.parse(JsonPath.read(body, "$.data.routes[0].stops[0].plannedArrival"));
        long firstLeg = Math.round(FakeMatrixProvider.meters(DEPOT, coordinate(stops.getFirst())) / 10);
        assertThat(firstArrival).isBetween(Instant.parse("2026-11-02T01:00:00Z").plusSeconds(firstLeg - 1),
                Instant.parse("2026-11-02T01:00:00Z").plusSeconds(firstLeg + 1));

        assertThat(JsonPath.<List<String>>read(body, "$.data.violations[*].code"))
                .containsExactly("CAPACITY_EXCEEDED", "SHIFT_EXCEEDED", "MAX_DISTANCE_EXCEEDED");
        assertThat(JsonPath.<Integer>read(body, "$.data.violations[0].excess")).isEqualTo(6);
        assertThat(JsonPath.<Integer>read(body, "$.data.violations[2].excess")).isEqualTo(distance - 6000);
        Instant end = Instant.parse(JsonPath.read(body, "$.data.routes[0].plannedEnd"));
        int over = JsonPath.read(body, "$.data.violations[1].excess");
        assertThat(end).isEqualTo(Instant.parse("2026-11-02T01:12:00Z").plusSeconds(over));
        assertThat(JsonPath.<Object>read(body, "$.data.violations[0].stopId")).isNull();
        // Read back: the same violations, computed from what was stored.
        get(store.dispatcher(), planId).andExpect(jsonPath("$.data.violations", hasSize(3)));
    }

    @Test
    void replaceRoutes_whenWindowedStopComesLate_reportTimeWindowMissed() throws Exception {
        TestStore store = storeWithDepot();
        // A must be served by 08:04; first, it is reached at about 08:02:30. B is about 3 km out.
        String stops = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"timeWindow\":{\"start\":\"08:00\",\"end\":\"08:04\"}},"
                + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22}]";
        long planId = optimizedPlan(store, stops, assignment(store.driver().id(), vehicle(store, "Xe 1")));
        long a = stopId(planId, "A");
        long b = stopId(planId, "B");

        String body = replace(store.owner(), planId, version(planId), List.of(List.of(b, a)), List.of(), routeIds(planId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<List<String>>read(body, "$.data.violations[*].code")).containsExactly("TIME_WINDOW_MISSED");
        assertThat(JsonPath.<Integer>read(body, "$.data.violations[0].stopId").longValue()).isEqualTo(a);
        assertThat(JsonPath.<Integer>read(body, "$.data.violations[0].excess")).isBetween(60, 600);
    }

    @Test
    void replaceRoutes_whenStopTakenOff_manualUntilOptimizedAgain() throws Exception {
        TestStore store = storeWithDepot();
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store));
        List<Long> stops = stopIds(planId);
        List<Long> kept = stops.subList(0, 2);

        replace(store.owner(), planId, version(planId), List.of(kept, List.of()), stops.subList(2, 4), routeIds(planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(2)))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("MANUAL"))
                .andExpect(jsonPath("$.data.violations", hasSize(0)));

        // Sent again as unassigned, they stay MANUAL.
        replace(store.owner(), planId, version(planId), List.of(kept, List.of()), stops.subList(2, 4), routeIds(planId))
                .andExpect(jsonPath("$.data.unassignedStops[1].dropReason").value("MANUAL"));

        optimize(store, planId);
        get(store.owner(), planId).andExpect(jsonPath("$.data.unassignedStops", hasSize(0)));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM route_plan_stops WHERE plan_id = ? AND manually_unassigned", Integer.class, planId)).isZero();
    }

    @Test
    void replaceRoutes_whenIdsWrong_400WithTheirFields() throws Exception {
        TestStore store = storeWithDepot();
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store));
        long otherPlan = optimizedPlan(store, FOUR_STOPS, twoAssignments(store), "2026-11-03");
        List<Long> routes = routeIds(planId);
        List<Long> stops = stopIds(planId);
        long foreignStop = stopIds(otherPlan).getFirst();
        long version = version(planId);

        // Unknown stop, a stop twice, one stop missing, one route missing, a route of another plan.
        String request = "{\"version\":%d,\"routes\":[{\"routeId\":%d,\"stopIds\":[%d,%d,%d]},{\"routeId\":%d,\"stopIds\":[]}],\"unassignedStopIds\":[%d]}"
                .formatted(version, routes.get(0), stops.get(0), foreignStop, stops.get(1), routeIds(otherPlan).getFirst(), stops.get(0));
        call(put("/api/v1/plans/" + planId + "/routes"), store.owner(), request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'routes[0].stopIds[1]')].code").value("INVALID_STOP_ID"))
                .andExpect(jsonPath("$.details[?(@.field == 'unassignedStopIds[0]')].code").value("INVALID_STOP_ID"))
                .andExpect(jsonPath("$.details[?(@.field == 'routes[1].routeId')].code").value("INVALID_ROUTE_ID"))
                .andExpect(jsonPath("$.details[?(@.field == 'routes')].code").value("ROUTES_INCOMPLETE"))
                .andExpect(jsonPath("$.details[?(@.field == 'stops')].code").value("STOPS_INCOMPLETE"));

        call(put("/api/v1/plans/" + planId + "/routes"), store.owner(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'version')].code").value("VERSION_REQUIRED"))
                .andExpect(jsonPath("$.details[?(@.field == 'routes')].code").value("ROUTES_REQUIRED"))
                .andExpect(jsonPath("$.details[?(@.field == 'unassignedStopIds')].code").value("UNASSIGNED_STOP_IDS_REQUIRED"));
        assertThat(version(planId)).isEqualTo(version);
    }

    @Test
    void replaceRoutes_whenPlanNotEditableOrStaleOrNotOptimized_refuse() throws Exception {
        TestStore store = storeWithDepot();
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store));
        List<Long> routes = routeIds(planId);
        List<List<Long>> current = currentRoutes(planId, routes);
        long version = version(planId);

        replace(store.owner(), planId, version - 1, current, List.of(), routes)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));

        long draft = createPlan(store, FOUR_STOPS, twoAssignments(store), "2026-11-04");
        replace(store.owner(), draft, version(draft), List.of(List.of(), List.of()), stopIds(draft), routeIds(draft))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("PLAN_NOT_OPTIMIZED"));

        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andExpect(status().isOk());
        replace(store.owner(), planId, version(planId), current, List.of(), routes)
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
    }

    @Test
    void replaceRoutes_whenMatrixFails_502AndNothingChanges() throws Exception {
        TestStore store = storeWithDepot();
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store));
        List<Long> routes = routeIds(planId);
        List<List<Long>> before = currentRoutes(planId, routes);
        long version = version(planId);
        matrixProvider.failNext();

        replace(store.owner(), planId, version, List.of(stopIds(planId), List.of()), List.of(), routes)
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("ROUTING_PROVIDER_ERROR"));

        assertThat(version(planId)).isEqualTo(version);
        assertThat(currentRoutes(planId, routes)).isEqualTo(before);
    }

    @Test
    void replaceRoutes_whenPlanOfAnotherStoreOrCallerIsDriver_404Or403() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        long planId = optimizedPlan(store, FOUR_STOPS, twoAssignments(store));
        List<Long> routes = routeIds(planId);
        List<List<Long>> current = currentRoutes(planId, routes);

        replace(otherStore.owner(), planId, version(planId), current, List.of(), routes)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        replace(store.driver(), planId, version(planId), current, List.of(), routes).andExpect(status().isForbidden());
    }

    private ResultActions replace(TestUser caller, long planId, long version, List<List<Long>> stopsByRoute,
                                  List<Long> unassigned, List<Long> routeIds) throws Exception {
        List<String> routes = new ArrayList<>();
        for (int i = 0; i < stopsByRoute.size(); i++) {
            routes.add("{\"routeId\":%d,\"stopIds\":%s}".formatted(routeIds.get(i), ids(stopsByRoute.get(i))));
        }
        String body = "{\"version\":%d,\"routes\":[%s],\"unassignedStopIds\":%s}"
                .formatted(version, String.join(",", routes), ids(unassigned));
        return call(put("/api/v1/plans/" + planId + "/routes"), caller, body);
    }

    private static String ids(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    private ResultActions get(TestUser caller, long planId) throws Exception {
        return call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/plans/" + planId), caller, null);
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               TestUser caller, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, caller.bearer());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    /**
     * Straight-line metres of depot → stops in order → depot, as the fake matrix measures them.
     */
    private double legsMeters(long planId, List<Long> stops) {
        double meters = 0;
        Coordinate from = DEPOT;
        for (long stopId : stops) {
            Coordinate to = coordinate(stopId);
            meters += FakeMatrixProvider.meters(from, to);
            from = to;
        }
        return meters + FakeMatrixProvider.meters(from, DEPOT);
    }

    private Coordinate coordinate(long stopId) {
        return jdbcTemplate.queryForObject("SELECT lat, lon FROM route_plan_stops WHERE id = ?",
                (rs, n) -> new Coordinate(rs.getDouble("lat"), rs.getDouble("lon")), stopId);
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

    private long vehicle(TestStore store, String name) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":10,\"capacityUnit\":\"BOX\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private String twoAssignments(TestStore store) throws Exception {
        return twoAssignments(store, "17:00", null);
    }

    private String twoAssignments(TestStore store, String shiftEnd, Integer maxDistanceMeters) throws Exception {
        TestUser second = users.addMember(store.id(), uk.huy.pathwise.shared.identity.UserRole.DRIVER, false);
        String limits = "\"shiftStart\":\"08:00\",\"shiftEnd\":\"%s\"%s".formatted(shiftEnd,
                maxDistanceMeters == null ? "" : ",\"maxDistanceMeters\":" + maxDistanceMeters);
        return "{\"driverId\":%d,\"vehicleId\":%d,%s}".formatted(store.driver().id(), vehicle(store, "Xe " + System.nanoTime()), limits)
                + "," + "{\"driverId\":%d,\"vehicleId\":%d,%s}".formatted(second.id(), vehicle(store, "Xe " + System.nanoTime()), limits);
    }

    private long createPlan(TestStore store, String stops, String assignments, String day) throws Exception {
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Giao sáng\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(day, stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private long optimizedPlan(TestStore store, String stops, String assignments) throws Exception {
        return optimizedPlan(store, stops, assignments, "2026-11-02");
    }

    private long optimizedPlan(TestStore store, String stops, String assignments, String day) throws Exception {
        long planId = createPlan(store, stops, assignments, day);
        optimize(store, planId);
        return planId;
    }

    private void optimize(TestStore store, long planId) throws Exception {
        mockMvc.perform(post("/api/v1/plans/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    private List<Long> routeIds(long planId) {
        return jdbcTemplate.queryForList("SELECT id FROM route_plan_routes WHERE plan_id = ? ORDER BY id", Long.class, planId);
    }

    private List<Long> stopIds(long planId) {
        return jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE plan_id = ? ORDER BY id", Long.class, planId);
    }

    private long stopId(long planId, String label) {
        return jdbcTemplate.queryForObject("SELECT id FROM route_plan_stops WHERE plan_id = ? AND label = ?", Long.class, planId, label);
    }

    /**
     * @return the stops of each route in {@code routes} order, by sequence
     */
    private List<List<Long>> currentRoutes(long planId, List<Long> routes) {
        return routes.stream()
                .map(routeId -> jdbcTemplate.queryForList(
                        "SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence", Long.class, routeId))
                .toList();
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }
}
