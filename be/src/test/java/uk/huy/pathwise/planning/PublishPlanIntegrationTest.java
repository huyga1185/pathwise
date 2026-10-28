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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T26 acceptance: publish with the shift overlap check (FR-P05, D-23, BR-12) and cancel (FR-P06). Plans are optimized
 * inline (the default) with the fake matrix, so each helper call returns a plan ready to publish.
 */
@AutoConfigureMockMvc
class PublishPlanIntegrationTest extends AbstractIntegrationTest {
    private static final String PLANS = "/api/v1/plans";
    private static final String DAY = "2026-11-02";
    private static final String STOP = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,\"demands\":{\"BOX\":5}}]";

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
    void publish_whenNotOptimized_return422PlanNotOptimized() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long planId = createPlan(store, DAY, STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));

        publish(store.owner(), planId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_OPTIMIZED"));
    }

    @Test
    void publish_whenOptimized_publishAndFreezeThePlan() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long planId = readyPlan(store, DAY, STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));
        long version = version(planId);

        publish(store.dispatcher(), planId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.version").value(version + 1))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(1)));

        publish(store.owner(), planId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
        mockMvc.perform(put(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version(planId) + ",\"name\":\"P\",\"planDate\":\"" + DAY + "\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
        mockMvc.perform(post(PLANS + "/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
        assertPublishedAt(planId);
    }

    @Test
    void publish_whenDriverShiftOverlapsPublishedPlan_return422ButBackToBackShiftWorks() throws Exception {
        TestStore store = storeWithDepot();
        long vehicleA = vehicle(store, "Xe A", 10);
        long vehicleB = vehicle(store, "Xe B", 10);
        long vehicleC = vehicle(store, "Xe C", 10);
        long driver = store.driver().id();
        long morning = readyPlan(store, DAY, STOP, assignment(driver, vehicleA, "08:00", "12:00"));
        publish(store.owner(), morning).andExpect(status().isOk());
        long overlapping = readyPlan(store, DAY, STOP, assignment(driver, vehicleB, "11:00", "15:00"));
        long afternoon = readyPlan(store, DAY, STOP, assignment(driver, vehicleC, "12:00", "16:00"));

        publish(store.owner(), overlapping)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SHIFT_OVERLAP"))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].field").value("assignments[0].driverId"))
                .andExpect(jsonPath("$.details[0].code").value("SHIFT_OVERLAP"))
                .andExpect(jsonPath("$.details[0].conflictPlanId").value(morning));
        publish(store.owner(), afternoon).andExpect(status().isOk());
    }

    @Test
    void publish_whenTwoOverlappingPlansPublishedAtOnce_onlyOneSucceeds() throws Exception {
        TestStore store = storeWithDepot();
        long driver = store.driver().id();
        long first = readyPlan(store, DAY, STOP, assignment(driver, vehicle(store, "Xe 1", 10), "08:00", "12:00"));
        long second = readyPlan(store, DAY, STOP, assignment(driver, vehicle(store, "Xe 2", 10), "10:00", "14:00"));
        long firstVersion = version(first);
        long secondVersion = version(second);
        CyclicBarrier start = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> a = pool.submit(() -> {
                start.await();
                return publishAt(store.owner(), first, firstVersion);
            });
            Future<Integer> b = pool.submit(() -> {
                start.await();
                return publishAt(store.owner(), second, secondVersion);
            });

            assertThat(List.of(a.get(), b.get())).containsExactlyInAnyOrder(200, 422);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void publish_whenVehicleShiftOverlaps_return422OnVehicle() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        TestUser otherDriver = users.addMember(store.id(), UserRole.DRIVER, false);
        long first = readyPlan(store, DAY, STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));
        publish(store.owner(), first).andExpect(status().isOk());
        long second = readyPlan(store, DAY, STOP, assignment(otherDriver.id(), vehicle, "09:00", "10:00"));

        publish(store.owner(), second)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.details[0].field").value("assignments[0].vehicleId"))
                .andExpect(jsonPath("$.details[0].conflictPlanId").value(first));
    }

    @Test
    void publish_whenOverlapOnAnotherDayOrWithCancelledPlan_publish() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long driver = store.driver().id();
        long today = readyPlan(store, DAY, STOP, assignment(driver, vehicle, "08:00", "12:00"));
        publish(store.owner(), today).andExpect(status().isOk());
        long tomorrow = readyPlan(store, "2026-11-03", STOP, assignment(driver, vehicle, "08:00", "12:00"));
        publish(store.owner(), tomorrow).andExpect(status().isOk());

        cancel(store.owner(), today, version(today)).andExpect(status().isOk());
        long replacement = readyPlan(store, DAY, STOP, assignment(driver, vehicle, "08:00", "12:00"));
        publish(store.owner(), replacement).andExpect(status().isOk());
    }

    @Test
    void publish_whenOverlappingRouteHasNoStops_ignoreIt() throws Exception {
        TestStore store = storeWithDepot();
        // The stop needs 5 boxes: only the big vehicle takes it, so the small one's route stays empty.
        long big = vehicle(store, "Xe lớn", 10);
        long small = vehicle(store, "Xe nhỏ", 1);
        TestUser idleDriver = users.addMember(store.id(), UserRole.DRIVER, false);
        long first = readyPlan(store, DAY, STOP, assignment(store.driver().id(), big, "08:00", "12:00") + ","
                + assignment(idleDriver.id(), small, "08:00", "12:00"));
        publish(store.owner(), first).andExpect(status().isOk());
        TestUser thirdDriver = users.addMember(store.id(), UserRole.DRIVER, false);
        long otherBig = vehicle(store, "Xe lớn 2", 10);

        // Uses the idle driver and the small vehicle of the first plan, which are free: their route has no stop.
        long second = readyPlan(store, DAY, STOP, assignment(idleDriver.id(), otherBig, "09:00", "11:00") + ","
                + assignment(thirdDriver.id(), small, "09:00", "11:00"));
        publish(store.owner(), second).andExpect(status().isOk());
    }

    @Test
    void publish_whenDriverOfUsedRouteDisabled_return422InvalidAssignment() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long planId = readyPlan(store, DAY, STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", store.driver().id());

        publish(store.owner(), planId)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"))
                .andExpect(jsonPath("$.details[0].field").value("assignments[0].driverId"));
    }

    @Test
    void cancel_whenDraftOrPublished_cancelButNotOnceDeliveryStarted() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = vehicle(store, "Xe 1", 10);
        long draft = createPlan(store, DAY, STOP, "");
        long published = readyPlan(store, "2026-11-04", STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));
        publish(store.owner(), published).andExpect(status().isOk());
        long running = readyPlan(store, "2026-11-05", STOP, assignment(store.driver().id(), vehicle, "08:00", "12:00"));
        publish(store.owner(), running).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE route_plans SET status = 'IN_PROGRESS' WHERE id = ?", running);

        cancel(store.dispatcher(), draft, 0)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        cancel(store.owner(), published, version(published))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.optimizedAt").isNotEmpty());
        cancel(store.owner(), running, version(running))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_CANCELLABLE"));
        cancel(store.owner(), draft, version(draft))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_CANCELLABLE"));
    }

    @Test
    void cancel_whenStaleVersionOrBusy_return409() throws Exception {
        TestStore store = storeWithDepot();
        long planId = createPlan(store, DAY, STOP, "");

        cancel(store.owner(), planId, 9)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));
        jdbcTemplate.update("UPDATE route_plans SET processing_status = 'GEOCODING' WHERE id = ?", planId);
        cancel(store.owner(), planId, 0)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
    }

    @Test
    void publishAndCancel_whenPlanOfAnotherStoreOrDriver_return404Or403() throws Exception {
        TestStore storeA = storeWithDepot();
        TestStore storeB = storeWithDepot();
        long planId = createPlan(storeA, DAY, STOP, "");

        publish(storeB.owner(), planId).andExpect(status().isNotFound());
        cancel(storeB.owner(), planId, 0).andExpect(status().isNotFound());
        publish(storeA.driver(), planId).andExpect(status().isForbidden());
        cancel(storeA.driver(), planId, 0).andExpect(status().isForbidden());
        mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, storeA.owner().bearer()))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    private void assertPublishedAt(long planId) {
        Object publishedAt = jdbcTemplate.queryForObject("SELECT published_at FROM route_plans WHERE id = ?", Object.class, planId);
        assertThat(publishedAt).isNotNull();
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
        return id(body);
    }

    private long createPlan(TestStore store, String day, String stops, String assignments) throws Exception {
        String body = mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"P\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(day, stops, assignments)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    /**
     * A draft optimized inline, ready to publish.
     */
    private long readyPlan(TestStore store, String day, String stops, String assignments) throws Exception {
        long planId = createPlan(store, day, stops, assignments);
        mockMvc.perform(post(PLANS + "/" + planId + "/optimize").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        return planId;
    }

    private static String assignment(long driverId, long vehicleId, String shiftStart, String shiftEnd) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"%s\",\"shiftEnd\":\"%s\"}".formatted(driverId, vehicleId, shiftStart, shiftEnd);
    }

    private ResultActions publish(TestUser caller, long planId) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version(planId) + "}"));
    }

    private int publishAt(TestUser caller, long planId, long version) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/publish").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andReturn().getResponse().getStatus();
    }

    private ResultActions cancel(TestUser caller, long planId, long version) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/cancel").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"));
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }

    private static long id(String body) {
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }
}
