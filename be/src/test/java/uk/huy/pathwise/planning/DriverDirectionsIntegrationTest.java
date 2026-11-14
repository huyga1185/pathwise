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
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeDirectionsProvider;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T33 acceptance: directions from the driver's position (FR-D05, P-57, API_SPEC §8).
 */
@AutoConfigureMockMvc
class DriverDirectionsIntegrationTest extends AbstractIntegrationTest {
    private static final String DAY = "2026-11-02";
    /** Both fit one vehicle of 10 boxes: one assignment makes one route with two stops. */
    private static final String TWO_SMALL_STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":3}},"
            + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22,\"demands\":{\"BOX\":3}}]";
    /** 6 boxes each for vehicles of 10: two assignments make two routes of one stop each. */
    private static final String TWO_LARGE_STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":6}},"
            + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22,\"demands\":{\"BOX\":6}}]";
    private static final double LAT = 16.0544;
    private static final double LON = 108.2022;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private FakeDirectionsProvider directionsProvider;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        directionsProvider.reset();
    }

    @Test
    void directions_whenNoStopGiven_fromPositionToFirstPendingStopOutsideTransaction() throws Exception {
        TestStore store = storeWithDepot();
        long routeId = routeOf(publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1"))));
        List<Long> stops = stopsOf(routeId);

        directions(store.driver(), routeId, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destination.stopId").value(stops.getFirst()))
                .andExpect(jsonPath("$.data.destination.lat").value(stopLat(stops.getFirst())))
                .andExpect(jsonPath("$.data.distanceMeters").value(1500))
                .andExpect(jsonPath("$.data.durationSeconds").value(180))
                .andExpect(jsonPath("$.data.polyline").value(FakeDirectionsProvider.POLYLINE))
                .andExpect(jsonPath("$.data.steps[0].maneuver").value("DEPART"))
                .andExpect(jsonPath("$.data.steps[0].roadName").value("Lê Duẩn"))
                .andExpect(jsonPath("$.data.steps[0].location.lat").value(LAT))
                .andExpect(jsonPath("$.data.steps[1].maneuver").value("ARRIVE"))
                .andExpect(jsonPath("$.data.steps[1].roadName").value((Object) null))
                .andExpect(jsonPath("$.data.steps[1].exitNumber").value((Object) null));

        Coordinate[] call = directionsProvider.lastCall();
        assertThat(call[0]).isEqualTo(new Coordinate(LAT, LON));
        assertThat(call[1]).isEqualTo(new Coordinate(stopLat(stops.getFirst()), stopLon(stops.getFirst())));
        assertThat(directionsProvider.lastCallInTransaction()).isFalse();
    }

    @Test
    void directions_whenFirstStopFinished_toNextPendingStop() throws Exception {
        TestStore store = storeWithDepot();
        long routeId = routeOf(publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1"))));
        List<Long> stops = stopsOf(routeId);
        deliver(store.driver(), routeId, stops.getFirst());

        directions(store.driver(), routeId, "").andExpect(jsonPath("$.data.destination.stopId").value(stops.get(1)));
    }

    @Test
    void directions_whenToStopId_toThatStopEvenIfFinished() throws Exception {
        TestStore store = storeWithDepot();
        long routeId = routeOf(publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1"))));
        List<Long> stops = stopsOf(routeId);
        deliver(store.driver(), routeId, stops.getFirst());

        directions(store.driver(), routeId, "&toStopId=" + stops.getFirst())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destination.stopId").value(stops.getFirst()));
    }

    @Test
    void directions_whenRouteHasNoPendingStop_toEndOfRoute() throws Exception {
        TestStore store = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, DAY, TWO_LARGE_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1")) + ","
                + assignment(colleague.id(), vehicle(store, "Xe 2")));
        long routeId = routeOf(planId, store.driver());
        deliver(store.driver(), routeId, stopsOf(routeId).getFirst());

        // The colleague's stop is still pending, so the plan is IN_PROGRESS.
        directions(store.driver(), routeId, "").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.destination.stopId").value((Object) null))
                .andExpect(jsonPath("$.data.destination.lat").value(16.05))
                .andExpect(jsonPath("$.data.destination.lon").value(108.2));
    }

    @Test
    void directions_whenToStopIdOfAnotherRoute_404() throws Exception {
        TestStore store = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, DAY, TWO_LARGE_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1")) + ","
                + assignment(colleague.id(), vehicle(store, "Xe 2")));
        long colleagueStop = stopsOf(routeOf(planId, colleague)).getFirst();

        directions(store.driver(), routeOf(planId, store.driver()), "&toStopId=" + colleagueStop)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        assertThat(directionsProvider.lastCall()).isNull();
    }

    @Test
    void directions_whenRouteOfAnotherDriverOrStoreOrDraft_404() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long vehicle = vehicle(store, "Xe 1");
        long routeId = routeOf(publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle)));
        long draftRoute = routeOf(optimizedPlan(store, "2026-11-04", TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle)));

        directions(colleague, routeId, "").andExpect(status().isNotFound());
        directions(otherStore.driver(), routeId, "").andExpect(status().isNotFound());
        directions(store.driver(), draftRoute, "").andExpect(status().isNotFound());
        assertThat(directionsProvider.lastCall()).isNull();
    }

    @Test
    void directions_whenPlanCompletedOrCancelled_planNotActive() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1");
        long completed = publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle));
        jdbcTemplate.update("UPDATE route_plans SET status = 'COMPLETED' WHERE id = ?", completed);
        long cancelled = publishedPlan(store, "2026-11-05", TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle));
        cancel(store, cancelled);

        directions(store.driver(), routeOf(completed), "").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
        directions(store.driver(), routeOf(cancelled), "").andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
    }

    @Test
    void directions_whenInvalidQuery_400() throws Exception {
        TestStore store = storeWithDepot();
        String route = "/api/v1/driver/routes/1/directions";

        mockMvc.perform(get(route + "?lat=91&lon=181").header(HttpHeaders.AUTHORIZATION, store.driver().bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'lat')].code").value("INVALID_LATITUDE"))
                .andExpect(jsonPath("$.details[?(@.field == 'lon')].code").value("INVALID_LONGITUDE"));
        mockMvc.perform(get(route + "?lat=16.05").header(HttpHeaders.AUTHORIZATION, store.driver().bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mockMvc.perform(get(route + "?lat=16.05&lon=108.2&toStopId=next").header(HttpHeaders.AUTHORIZATION, store.driver().bearer()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void directions_whenProviderFails_502() throws Exception {
        TestStore store = storeWithDepot();
        long routeId = routeOf(publishedPlan(store, DAY, TWO_SMALL_STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1"))));
        directionsProvider.failNext();

        directions(store.driver(), routeId, "").andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("ROUTING_PROVIDER_ERROR"));
    }

    @Test
    void directions_whenCallerCannotDrive_403() throws Exception {
        TestStore store = storeWithDepot();

        directions(store.dispatcher(), 1, "").andExpect(status().isForbidden());
    }

    private ResultActions directions(TestUser caller, long routeId, String extraQuery) throws Exception {
        return mockMvc.perform(get("/api/v1/driver/routes/" + routeId + "/directions?lat=" + LAT + "&lon=" + LON + extraQuery)
                .header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private void deliver(TestUser driver, long routeId, long stopId) throws Exception {
        mockMvc.perform(patch("/api/v1/driver/routes/" + routeId + "/stops/" + stopId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, driver.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isOk());
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

    private long optimizedPlan(TestStore store, String day, String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Giao sáng\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(day, stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        mockMvc.perform(post("/api/v1/plans/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        return planId;
    }

    private long publishedPlan(TestStore store, String day, String stops, String assignments) throws Exception {
        long planId = optimizedPlan(store, day, stops, assignments);
        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());
        return planId;
    }

    private void cancel(TestStore store, long planId) throws Exception {
        mockMvc.perform(post("/api/v1/plans/" + planId + "/cancel").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());
    }

    /**
     * @return the plan's only route that has stops
     */
    private long routeOf(long planId) {
        return jdbcTemplate.queryForObject("SELECT DISTINCT route_id FROM route_plan_stops WHERE plan_id = ? AND route_id IS NOT NULL",
                Long.class, planId);
    }

    private long routeOf(long planId, TestUser driver) {
        return jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ? AND driver_id = ?",
                Long.class, planId, driver.id());
    }

    private List<Long> stopsOf(long routeId) {
        return jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence", Long.class, routeId);
    }

    private double stopLat(long stopId) {
        return jdbcTemplate.queryForObject("SELECT lat FROM route_plan_stops WHERE id = ?", Double.class, stopId);
    }

    private double stopLon(long stopId) {
        return jdbcTemplate.queryForObject("SELECT lon FROM route_plan_stops WHERE id = ?", Double.class, stopId);
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }
}
