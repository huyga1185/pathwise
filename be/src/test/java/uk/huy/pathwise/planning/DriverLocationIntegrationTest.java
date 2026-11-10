package uk.huy.pathwise.planning;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T32 acceptance: driver positions in batches (D-26, P-15), the sharing state (P-53) and what the dispatcher reads
 * (FR-D04, API_SPEC §7 and §8).
 */
@AutoConfigureMockMvc
class DriverLocationIntegrationTest extends AbstractIntegrationTest {
    private static final String LOCATION = "/api/v1/driver/location";
    private static final String SHARING = "/api/v1/driver/location/sharing";
    private static final String DAY = "2026-11-02";
    /** 6 boxes each for vehicles of 10: two assignments make two routes with stops; one leaves a stop unplaced. */
    private static final String STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":6}},"
            + "{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22,\"demands\":{\"BOX\":6}}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private TestUsers users;
    private Instant now;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }

    @Test
    void recordLocations_whenBatchOutOfOrder_keepNewestPoint() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));

        send(store.driver(), point(16.01, 108.01, now.minusSeconds(40), ""),
                point(16.03, 108.03, now.minusSeconds(10), ",\"accuracyMeters\":12.5,\"headingDegrees\":90,\"speedMps\":7.8"),
                point(16.02, 108.02, now.minusSeconds(20), ""))
                .andExpect(status().isNoContent());

        locations(store.dispatcher(), planId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].routeId").value(routeOf(planId)))
                .andExpect(jsonPath("$.data[0].driverId").value(store.driver().id()))
                .andExpect(jsonPath("$.data[0].location.lat").value(16.03))
                .andExpect(jsonPath("$.data[0].location.lon").value(108.03))
                .andExpect(jsonPath("$.data[0].location.accuracyMeters").value(12.5))
                .andExpect(jsonPath("$.data[0].location.headingDegrees").value(90.0))
                .andExpect(jsonPath("$.data[0].location.speedMps").value(7.8))
                .andExpect(jsonPath("$.data[0].location.recordedAt").value(now.minusSeconds(10).toString()))
                .andExpect(jsonPath("$.data[0].sharing").value((Object) null));
    }

    @Test
    void recordLocations_whenLaterBatchHasOlderPoint_keepStoredPoint() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));

        send(store.driver(), point(16.03, 108.03, now.minusSeconds(10), "")).andExpect(status().isNoContent());
        // Sent late after a network outage.
        send(store.driver(), point(16.01, 108.01, now.minusSeconds(60), "")).andExpect(status().isNoContent());

        locations(store.owner(), planId).andExpect(jsonPath("$.data[0].location.lat").value(16.03));
    }

    @Test
    void recordLocations_whenPointAheadOfServerClock_ignoredAndLaterPointsStillStored() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));

        // A phone clock an hour fast: accepted, but stored it would hide every real point for an hour.
        send(store.driver(), point(16.09, 108.09, now.plus(Duration.ofHours(1)), "")).andExpect(status().isNoContent());
        locations(store.owner(), planId).andExpect(jsonPath("$.data[0].location").value((Object) null));

        send(store.driver(), point(16.03, 108.03, now.plusSeconds(60), ""), point(16.02, 108.02, now, ""))
                .andExpect(status().isNoContent());
        locations(store.owner(), planId).andExpect(jsonPath("$.data[0].location.lat").value(16.03));
    }

    @Test
    void planLocations_whenPointOlderThanTenMinutes_notReturned() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));

        // Recorded 11 minutes ago: the batch is accepted, the point is not kept.
        send(store.driver(), point(16.03, 108.03, now.minus(Duration.ofMinutes(11)), "")).andExpect(status().isNoContent());
        assertThat(redisTemplate.hasKey(locationKey(store, store.driver()))).isFalse();

        // Stored while fresh but read after it aged (the key outlives the point by a little).
        redisTemplate.opsForHash().putAll(locationKey(store, store.driver()), Map.of("t", "1",
                "v", "{\"lat\":16.03,\"lon\":108.03,\"recordedAt\":\"" + now.minus(Duration.ofMinutes(10)).minusSeconds(1) + "\"}"));
        redisTemplate.expire(locationKey(store, store.driver()), Duration.ofMinutes(1));
        locations(store.owner(), planId).andExpect(jsonPath("$.data[0].location").value((Object) null));
    }

    @Test
    void recordLocations_whenPointStored_keyExpiresTenMinutesAfterRecording() throws Exception {
        TestStore store = storeWithDepot();
        publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));

        send(store.driver(), point(16.03, 108.03, now.minus(Duration.ofMinutes(4)), "")).andExpect(status().isNoContent());

        Long ttl = redisTemplate.getExpire(locationKey(store, store.driver()));
        assertThat(ttl).isBetween(Duration.ofMinutes(5).toSeconds(), Duration.ofMinutes(6).toSeconds());
    }

    @Test
    void recordLocations_whenInvalidBatch_400WithFieldCodes() throws Exception {
        TestStore store = storeWithDepot();

        send(store.driver()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].code", hasItem("POINTS_REQUIRED")));
        call(post(LOCATION), store.driver(), "{\"points\":[{\"lat\":91,\"lon\":-181,\"headingDegrees\":360,\"speedMps\":-1,\"accuracyMeters\":-1}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].lat')].code").value("INVALID_LATITUDE"))
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].lon')].code").value("INVALID_LONGITUDE"))
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].headingDegrees')].code").value("INVALID_HEADING"))
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].speedMps')].code").value("INVALID_SPEED"))
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].accuracyMeters')].code").value("INVALID_ACCURACY"))
                .andExpect(jsonPath("$.details[?(@.field == 'points[0].recordedAt')].code").value("RECORDED_AT_REQUIRED"));
        String[] many = IntStream.range(0, 101).mapToObj(i -> point(16.0, 108.0, now.minusSeconds(i), "")).toArray(String[]::new);
        send(store.driver(), many).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[*].code", hasItem("TOO_MANY_POINTS")));
    }

    @Test
    void driverLocation_whenCallerCannotDrive_403() throws Exception {
        TestStore store = storeWithDepot();

        send(store.dispatcher(), point(16.0, 108.0, now, "")).andExpect(status().isForbidden());
        call(put(SHARING), store.dispatcher(), "{\"routeId\":1,\"state\":\"SHARING\"}").andExpect(status().isForbidden());
        locations(store.driver(), 1).andExpect(status().isForbidden());
    }

    @Test
    void updateSharing_whenOwnRoute_dispatcherSeesStateOnThatRouteOnly() throws Exception {
        TestStore store = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")) + ","
                + assignment(colleague.id(), vehicle(store, "Xe 2")));
        long routeId = routeOf(planId, store.driver());

        call(put(SHARING), store.driver(), "{\"routeId\":" + routeId + ",\"state\":\"GPS_OFF\"}").andExpect(status().isNoContent());

        String body = locations(store.dispatcher(), planId).andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(body, "$.data[?(@.routeId == " + routeId + ")].sharing.state")).containsExactly("GPS_OFF");
        assertThat(JsonPath.<List<Object>>read(body, "$.data[?(@.routeId == " + routeId + ")].sharing.updatedAt")).doesNotContainNull();
        assertThat(JsonPath.<List<Object>>read(body, "$.data[?(@.driverId == " + colleague.id() + ")].sharing")).containsExactly((Object) null);

        Long ttl = redisTemplate.getExpire("driver:sharing:" + store.id() + ":" + store.driver().id());
        assertThat(ttl).isBetween(Duration.ofHours(11).toSeconds(), Duration.ofHours(12).toSeconds());
    }

    @Test
    void updateSharing_whenStateForAnotherRoute_notShownOnThisPlan() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1");
        long morning = publishedPlan(store, "2026-11-02", assignment(store.driver().id(), vehicle));
        long nextDay = publishedPlan(store, "2026-11-03", assignment(store.driver().id(), vehicle));

        call(put(SHARING), store.driver(), "{\"routeId\":" + routeOf(nextDay) + ",\"state\":\"DECLINED\"}")
                .andExpect(status().isNoContent());

        locations(store.owner(), morning).andExpect(jsonPath("$.data[0].sharing").value((Object) null));
        locations(store.owner(), nextDay).andExpect(jsonPath("$.data[0].sharing.state").value("DECLINED"));
    }

    @Test
    void updateSharing_whenRouteOfAnotherDriverOrStoreOrDraft_404() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        TestUser colleague = users.addMember(store.id(), UserRole.DRIVER, false);
        long vehicle = vehicle(store, "Xe 1");
        long routeId = routeOf(publishedPlan(store, assignment(store.driver().id(), vehicle)));
        long draftRoute = routeOf(optimizedPlan(store, "2026-11-04", assignment(store.driver().id(), vehicle)));

        sharing(colleague, routeId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        sharing(otherStore.driver(), routeId).andExpect(status().isNotFound());
        sharing(store.driver(), draftRoute).andExpect(status().isNotFound());
        assertThat(redisTemplate.hasKey("driver:sharing:" + store.id() + ":" + colleague.id())).isFalse();
    }

    @Test
    void updateSharing_whenPlanCompletedOrCancelled_planNotActive() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1");
        long completed = publishedPlan(store, assignment(store.driver().id(), vehicle));
        jdbcTemplate.update("UPDATE route_plans SET status = 'COMPLETED' WHERE id = ?", completed);
        long cancelled = publishedPlan(store, "2026-11-05", assignment(store.driver().id(), vehicle));
        cancel(store, cancelled);

        sharing(store.driver(), routeOf(completed)).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
        sharing(store.driver(), routeOf(cancelled)).andExpect(jsonPath("$.code").value("PLAN_NOT_ACTIVE"));
    }

    @Test
    void updateSharing_whenInvalidBody_400() throws Exception {
        TestStore store = storeWithDepot();

        call(put(SHARING), store.driver(), "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'routeId')].code").value("ROUTE_ID_REQUIRED"))
                .andExpect(jsonPath("$.details[?(@.field == 'state')].code").value("SHARING_STATE_REQUIRED"));
        call(put(SHARING), store.driver(), "{\"routeId\":1,\"state\":\"MAYBE\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void planLocations_whenPlanOfAnotherStore_404() throws Exception {
        TestStore store = storeWithDepot();
        TestStore otherStore = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));
        send(store.driver(), point(16.03, 108.03, now, "")).andExpect(status().isNoContent());

        locations(otherStore.dispatcher(), planId).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        locations(otherStore.owner(), planId).andExpect(status().isNotFound());
    }

    @Test
    void planLocations_whenPlanNotRunning_empty() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1");
        long draft = optimizedPlan(store, "2026-11-04", assignment(store.driver().id(), vehicle));
        long completed = publishedPlan(store, assignment(store.driver().id(), vehicle));
        send(store.driver(), point(16.03, 108.03, now, "")).andExpect(status().isNoContent());
        locations(store.owner(), completed).andExpect(jsonPath("$.data", hasSize(1)));

        jdbcTemplate.update("UPDATE route_plans SET status = 'COMPLETED' WHERE id = ?", completed);
        locations(store.owner(), completed).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
        locations(store.owner(), draft).andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void planDetail_whenDriverHasPhoneNumber_showItForCalling() throws Exception {
        TestStore store = storeWithDepot();
        long planId = publishedPlan(store, assignment(store.driver().id(), vehicle(store, "Xe 1")));
        jdbcTemplate.update("UPDATE users SET phone_number = '+84905123019' WHERE id = ?", store.driver().id());

        call(get("/api/v1/plans/" + planId), store.dispatcher(), null)
                .andExpect(jsonPath("$.data.routes[0].driver.phoneNumber").value("+84905123019"));
    }

    private static String point(double lat, double lon, Instant recordedAt, String extra) {
        return "{\"lat\":%s,\"lon\":%s,\"recordedAt\":\"%s\"%s}".formatted(lat, lon, recordedAt, extra);
    }

    private ResultActions send(TestUser driver, String... points) throws Exception {
        return call(post(LOCATION), driver, "{\"points\":[" + String.join(",", points) + "]}");
    }

    private ResultActions sharing(TestUser driver, long routeId) throws Exception {
        return call(put(SHARING), driver, "{\"routeId\":" + routeId + ",\"state\":\"SHARING\"}");
    }

    private ResultActions locations(TestUser caller, long planId) throws Exception {
        return call(get("/api/v1/plans/" + planId + "/driver-locations"), caller, null);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, TestUser caller, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, caller.bearer());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private static String locationKey(TestStore store, TestUser driver) {
        return "driver:location:" + store.id() + ":" + driver.id();
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

    private long optimizedPlan(TestStore store, String day, String assignments) throws Exception {
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Giao sáng\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(day, STOPS, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        mockMvc.perform(post("/api/v1/plans/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        return planId;
    }

    private long publishedPlan(TestStore store, String assignments) throws Exception {
        return publishedPlan(store, DAY, assignments);
    }

    private long publishedPlan(TestStore store, String day, String assignments) throws Exception {
        long planId = optimizedPlan(store, day, assignments);
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
     * @return the route of the plan's only route that has stops
     */
    private long routeOf(long planId) {
        return jdbcTemplate.queryForObject("SELECT DISTINCT route_id FROM route_plan_stops WHERE plan_id = ? AND route_id IS NOT NULL",
                Long.class, planId);
    }

    private long routeOf(long planId, TestUser driver) {
        return jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ? AND driver_id = ?",
                Long.class, planId, driver.id());
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}".formatted(driverId, vehicleId);
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }
}
