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

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T30 acceptance: the driver sees only their own routes with stops, in published plans (FR-D01, FR-D02, API_SPEC §8).
 */
@AutoConfigureMockMvc
class DriverRouteIntegrationTest extends AbstractIntegrationTest {
    private static final String ROUTES = "/api/v1/driver/routes";
    private static final String DAY = "2026-11-02";
    private static final String STOPS = """
            [{"label":"A","lat":16.06,"lon":108.21,"recipientName":"Chị Lan","recipientPhone":"0905 123 456",
              "address":"12 Lê Duẩn","note":"Gọi trước","timeWindow":{"start":"09:00","end":"11:00"},"demands":{"BOX":5}},
             {"label":"B","lat":16.07,"lon":108.22}]""";

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
    void routes_whenDriverOfPublishedPlan_listAndShowOwnRouteInOrder() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, DAY, STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));

        String list = listRoutes(store.driver(), "?date=" + DAY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].planId").value(planId))
                .andExpect(jsonPath("$.data[0].planName").value("Giao sáng"))
                .andExpect(jsonPath("$.data[0].planDate").value(DAY))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].stopCount").value(2))
                .andExpect(jsonPath("$.data[0].completedCount").value(0))
                .andExpect(jsonPath("$.data[0].plannedStart").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        long routeId = ((Number) JsonPath.read(list, "$.data[0].routeId")).longValue();

        getRoute(store.driver(), routeId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.routeId").value(routeId))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.start.address").value("Kho"))
                .andExpect(jsonPath("$.data.end.lat").value(16.05))
                .andExpect(jsonPath("$.data.stops[*].sequence", contains(1, 2)))
                .andExpect(jsonPath("$.data.stops[?(@.label == 'A')].recipientPhone").value("+84905123456"))
                .andExpect(jsonPath("$.data.stops[?(@.label == 'A')].timeWindow.start").value("09:00"))
                .andExpect(jsonPath("$.data.stops[?(@.label == 'A')].note").value("Gọi trước"))
                .andExpect(jsonPath("$.data.stops[0].plannedArrival").isNotEmpty())
                .andExpect(jsonPath("$.data.stops[0].navigationUrl").doesNotExist())
                .andExpect(jsonPath("$.data.stops[0].status").value("PENDING"));
    }

    @Test
    void routes_whenStopsFinish_routeStatusFollowsItsOwnStops() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, DAY, STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = onlyRouteId(store.driver());
        long[] stops = jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE plan_id = ? ORDER BY sequence", Long.class, planId)
                .stream().mapToLong(Long::longValue).toArray();

        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'DELIVERED' WHERE id = ?", stops[0]);
        getRoute(store.driver(), routeId).andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        listRoutes(store.driver(), "?date=" + DAY).andExpect(jsonPath("$.data[0].completedCount").value(1));

        jdbcTemplate.update("UPDATE route_plan_stops SET status = 'FAILED', failure_reason = 'Vắng nhà' WHERE id = ?", stops[1]);
        jdbcTemplate.update("UPDATE route_plans SET status = 'COMPLETED' WHERE id = ?", planId);
        getRoute(store.driver(), routeId)
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.stops[1].failureReason").value("Vắng nhà"));
    }

    @Test
    void routes_whenRouteOfAnotherDriverOrStore_notListedAnd404() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        publishedPlan(store, DAY, STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));
        long routeId = onlyRouteId(store.driver());

        listRoutes(colleague, "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(0)));
        getRoute(colleague, routeId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        getRoute(otherStore.driver(), routeId).andExpect(status().isNotFound());
    }

    @Test
    void routes_whenPlanDraftOrCancelled_hidden() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long draft = optimizedPlan(store, DAY, STOPS, assignment(store.driver().id(), vehicle));
        long draftRoute = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ?", Long.class, draft);

        listRoutes(store.driver(), "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(0)));
        getRoute(store.driver(), draftRoute).andExpect(status().isNotFound());

        publish(store, draft);
        listRoutes(store.driver(), "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(post("/api/v1/plans/" + draft + "/cancel").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(draft) + "}"))
                .andExpect(status().isOk());
        listRoutes(store.driver(), "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(0)));
        getRoute(store.driver(), draftRoute).andExpect(status().isNotFound());
    }

    @Test
    void routes_whenRouteHasNoStops_hiddenFromItsDriver() throws Exception {
        TestStore store = storeWithDepot();
        TestUser idle = users.addMember(store.id(), UserRole.DRIVER, false);
        // One stop of 5 boxes: only the big vehicle can carry it, so the idle driver's route stays empty.
        long planId = publishedPlan(store, DAY, "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":5}}]",
                assignment(store.driver().id(), vehicle(store, "Xe lớn", 10)) + "," + assignment(idle.id(), vehicle(store, "Xe nhỏ", 1)));
        long idleRoute = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ? AND driver_id = ?",
                Long.class, planId, idle.id());

        listRoutes(idle, "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(0)));
        getRoute(idle, idleRoute).andExpect(status().isNotFound());
        listRoutes(store.driver(), "?date=" + DAY).andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    void routes_whenOwnerWithCanDriveOrDispatcherWithout_seeOwnOr403() throws Exception {
        TestStore store = storeWithDepot();
        TestUser owner = users.withCanDrive(store.owner(), true);
        publishedPlan(store, DAY, STOPS, assignment(owner.id(), vehicle(store, "Xe 1", 10)));

        listRoutes(owner, "?date=" + DAY).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));
        listRoutes(store.dispatcher(), "?date=" + DAY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        getRoute(store.dispatcher(), 1).andExpect(status().isForbidden());
        listRoutes(users.createAdmin(), "").andExpect(status().isForbidden());
    }

    @Test
    void routes_whenNoDate_useTodayInStoreTimezone() throws Exception {
        TestStore store = storeWithDepot();
        String today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();
        publishedPlan(store, today, STOPS, assignment(store.driver().id(), vehicle(store, "Xe 1", 10)));

        listRoutes(store.driver(), "")
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].planDate").value(today));
        listRoutes(store.driver(), "?date=22-10-2026")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
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
        publish(store, planId);
        return planId;
    }

    private void publish(TestStore store, long planId) throws Exception {
        mockMvc.perform(post("/api/v1/plans/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isOk());
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    private long onlyRouteId(TestUser driver) throws Exception {
        String body = listRoutes(driver, "?date=" + DAY).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data[0].routeId")).longValue();
    }

    private ResultActions listRoutes(TestUser caller, String query) throws Exception {
        return mockMvc.perform(get(ROUTES + query).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private ResultActions getRoute(TestUser caller, long routeId) throws Exception {
        return mockMvc.perform(get(ROUTES + "/" + routeId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }
}
