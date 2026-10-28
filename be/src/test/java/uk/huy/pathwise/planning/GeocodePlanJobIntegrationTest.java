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
import uk.huy.pathwise.job.service.JobRecovery;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.FakeGeocodingProvider;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;
import uk.huy.pathwise.support.TestUsers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T29 acceptance: background geocoding of plans (FR-P09, ARCHITECTURE.md §8.2) on the job framework (§8.4), and
 * {@code GET /jobs/{id}}. Run once per job mode by the subclasses, to show that the mode does not change the
 * behavior; every test waits on a condition, so it holds for both.
 */
@AutoConfigureMockMvc
abstract class GeocodePlanJobIntegrationTest extends AbstractIntegrationTest {
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
    FakeGeocodingProvider geocoding;

    @Autowired
    JobRecovery jobRecovery;

    TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void create_whenStopsHaveOnlyAddresses_geocodeThemInBackgroundThenIdle() throws Exception {
        TestStore store = storeWithDepot();
        String a = address("12 Lê Duẩn");
        String b = address("1 Bạch Đằng");
        String c = address("5 Trần Phú");
        geocoding.resolve(a, 16.07, 108.22);
        geocoding.resolve(b, 16.08, 108.23);
        geocoding.resolve(c, 16.09, 108.24);
        String body = create(store.owner(), """
                [{"label":"A","address":"%s"},{"label":"P1","lat":16.1,"lon":108.1},{"label":"B","address":"%s"},
                 {"label":"P2","lat":16.2,"lon":108.2},{"label":"C","address":"%s"}]""".formatted(a, b, c))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.processingStatus").value("GEOCODING"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.activeJob.type").value("GEOCODE_PLAN"))
                .andExpect(jsonPath("$.data.activeJob.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.geocodingSummary.pending").value(3))
                .andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long jobId = ((Number) JsonPath.read(body, "$.data.activeJob.id")).longValue();

        awaitIdle(planId);
        getPlan(store.dispatcher(), planId)
                .andExpect(jsonPath("$.data.activeJob").value(nullValue()))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.geocodingSummary.resolved").value(3))
                .andExpect(jsonPath("$.data.geocodingSummary.provided").value(2))
                .andExpect(jsonPath("$.data.geocodingSummary.pending").value(0))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].lat").value(16.07))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodedDisplayName").value(a + ", Việt Nam"))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodeStatus").value("PROVIDED"))
                .andExpect(jsonPath("$.data.unassignedStops[4].lon").value(108.24));
        getJob(store.dispatcher(), jobId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.planId").value(planId))
                .andExpect(jsonPath("$.data.errorCode").value(nullValue()))
                .andExpect(jsonPath("$.data.startedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.finishedAt").isNotEmpty());
    }

    @Test
    void create_whenOnlyCoordinates_startNoJob() throws Exception {
        TestStore store = storeWithDepot();

        create(store.owner(), "[{\"label\":\"P\",\"lat\":16.1,\"lon\":108.1}]")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.processingStatus").value("IDLE"))
                .andExpect(jsonPath("$.data.activeJob").value(nullValue()));
    }

    @Test
    void job_whenAddressNotFoundOrProviderFails_failThatStopOnlyAndSucceed() throws Exception {
        TestStore store = storeWithDepot();
        String found = address("12 Lê Duẩn");
        String unknown = address("không có");
        String broken = address("lỗi");
        geocoding.resolve(found, 16.07, 108.22);
        geocoding.fail(broken);
        String body = create(store.owner(), """
                [{"label":"A","address":"%s"},{"label":"B","address":"%s"},{"label":"C","address":"%s"}]"""
                .formatted(unknown, found, broken)).andReturn().getResponse().getContentAsString();
        long planId = id(body);

        awaitIdle(planId);
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeError").value("NOT_FOUND"))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodeStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.unassignedStops[2].geocodeStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.unassignedStops[2].geocodeError").value("PROVIDER_ERROR"))
                .andExpect(jsonPath("$.data.geocodingSummary.failed").value(2));
        getJob(store.owner(), jobIdOf(body)).andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    @Test
    void job_whenRateLimitBusyFewTimes_retryAndResolve() throws Exception {
        TestStore store = storeWithDepot();
        String address = address("12 Lê Duẩn");
        geocoding.resolve(address, 16.07, 108.22);
        geocoding.busy(address, 2);
        long planId = id(create(store.owner(), "[{\"label\":\"A\",\"address\":\"" + address + "\"}]")
                .andReturn().getResponse().getContentAsString());

        awaitIdle(planId);
        getPlan(store.owner(), planId).andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("RESOLVED"));
        assertThat(geocoding.calls(address)).isEqualTo(3);
    }

    @Test
    void geocode_whenStopsFailed_retryThemAndReturnJob() throws Exception {
        TestStore store = storeWithDepot();
        String address = address("12 Lê Duẩn");
        long planId = id(create(store.owner(), "[{\"label\":\"A\",\"address\":\"" + address + "\"},{\"label\":\"P\",\"lat\":1,\"lon\":1}]")
                .andReturn().getResponse().getContentAsString());
        awaitIdle(planId);
        long version = version(planId);
        geocoding.resolve(address, 16.07, 108.22);

        String job = geocode(store.dispatcher(), planId, version)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.type").value("GEOCODE_PLAN"))
                .andExpect(jsonPath("$.data.planId").value(planId))
                .andReturn().getResponse().getContentAsString();

        awaitIdle(planId);
        getPlan(store.owner(), planId)
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeError").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodeStatus").value("PROVIDED"))
                // One for the retry request, one when its job finished.
                .andExpect(jsonPath("$.data.version").value(version + 2));
        getJob(store.owner(), id(job)).andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }

    @Test
    void geocode_whenNothingToGeocodeOrStaleVersion_return422Or409() throws Exception {
        TestStore store = storeWithDepot();
        long planId = id(create(store.owner(), "[{\"label\":\"P\",\"lat\":1,\"lon\":1}]")
                .andReturn().getResponse().getContentAsString());

        geocode(store.owner(), planId, 0)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NOTHING_TO_GEOCODE"));
        geocode(store.owner(), planId, 7)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));
        mockMvc.perform(post(PLANS + "/" + planId + "/geocode").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("VERSION_REQUIRED"));
    }

    @Test
    void patchStop_whenAddressChanges_startGeocodingJob() throws Exception {
        TestStore store = storeWithDepot();
        String address = address("12 Lê Duẩn");
        geocoding.resolve(address, 16.07, 108.22);
        String body = create(store.owner(), "[{\"label\":\"A\",\"lat\":1,\"lon\":1}]").andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long stopId = ((Number) JsonPath.read(body, "$.data.unassignedStops[0].id")).longValue();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch(PLANS + "/" + planId + "/stops/" + stopId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"address\":\"" + address + "\",\"lat\":null,\"lon\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.processingStatus").value("GEOCODING"))
                .andExpect(jsonPath("$.data.activeJob.status").value("QUEUED"));

        awaitIdle(planId);
        getPlan(store.owner(), planId).andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("RESOLVED"));
    }

    @Test
    void startup_whenJobsQueuedOrRunning_failThemInterruptedAndReleasePlans() throws Exception {
        TestStore store = storeWithDepot();
        long running = planWithJob(store, "RUNNING");
        long queued = planWithJob(store, "QUEUED");

        jobRecovery.recoverInterruptedJobs();

        for (long planId : new long[]{running, queued}) {
            assertThat(processingStatus(planId)).isEqualTo("IDLE");
            assertThat(jdbcTemplate.queryForObject("SELECT active_job_id FROM route_plans WHERE id = ?", Long.class, planId)).isNull();
            assertThat(jdbcTemplate.queryForMap("SELECT status, error_code FROM background_jobs WHERE target_id = ?", planId))
                    .containsEntry("status", "FAILED")
                    .containsEntry("error_code", "INTERRUPTED");
        }
        // Released, so the plan can be edited again.
        mockMvc.perform(put(PLANS + "/" + running).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version(running) + ",\"name\":\"P\",\"planDate\":\"2026-11-02\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void job_whenOfAnotherStore_return404AndDriverGets403() throws Exception {
        TestStore storeA = storeWithDepot();
        TestStore storeB = storeWithDepot();
        String body = create(storeA.owner(), "[{\"label\":\"A\",\"address\":\"" + address("x") + "\"}]")
                .andReturn().getResponse().getContentAsString();
        long jobId = jobIdOf(body);
        awaitIdle(id(body));

        getJob(storeB.owner(), jobId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("JOB_NOT_FOUND"));
        getJob(storeA.owner(), Long.MAX_VALUE).andExpect(status().isNotFound());
        getJob(storeA.driver(), jobId).andExpect(status().isForbidden());
        geocode(storeB.owner(), id(body), 0).andExpect(status().isNotFound());
        geocode(storeA.driver(), id(body), 0).andExpect(status().isForbidden());
    }

    /**
     * A plan marked as being processed by a job in {@code status}, as a crash would leave it.
     */
    private long planWithJob(TestStore store, String status) throws Exception {
        long planId = id(create(store.owner(), "[{\"label\":\"P\",\"lat\":1,\"lon\":1}]").andReturn().getResponse().getContentAsString());
        jdbcTemplate.update("INSERT INTO background_jobs (store_id, type, target_id, status, attempts, created_by, created_at, updated_at) "
                + "VALUES (?, 'GEOCODE_PLAN', ?, ?, 0, ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))", store.id(), planId, status, store.owner().id());
        long jobId = jdbcTemplate.queryForObject("SELECT id FROM background_jobs WHERE target_id = ?", Long.class, planId);
        jdbcTemplate.update("UPDATE route_plans SET processing_status = 'GEOCODING', active_job_id = ? WHERE id = ?", jobId, planId);
        return planId;
    }

    TestStore storeWithDepot() throws Exception {
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depotAddress\":\"Kho\",\"depotLat\":16.05,\"depotLon\":108.2,"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        return store;
    }

    /**
     * A unique address, so that tests do not share fake geocoding answers.
     */
    static String address(String street) {
        return street + " #" + UUID.randomUUID();
    }

    void awaitIdle(long planId) {
        await().atMost(Duration.ofSeconds(20)).until(() -> processingStatus(planId).equals("IDLE"));
    }

    String processingStatus(long planId) {
        return jdbcTemplate.queryForObject("SELECT processing_status FROM route_plans WHERE id = ?", String.class, planId);
    }

    long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }

    ResultActions create(TestUser caller, String stops) throws Exception {
        return mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"P\",\"planDate\":\"2026-11-02\",\"stops\":" + stops + "}"));
    }

    ResultActions getPlan(TestUser caller, long planId) throws Exception {
        return mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    ResultActions getJob(TestUser caller, long jobId) throws Exception {
        return mockMvc.perform(get("/api/v1/jobs/" + jobId).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    ResultActions geocode(TestUser caller, long planId, long version) throws Exception {
        return mockMvc.perform(post(PLANS + "/" + planId + "/geocode").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"));
    }

    static long id(String body) {
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    static long jobIdOf(String body) {
        return ((Number) JsonPath.read(body, "$.data.activeJob.id")).longValue();
    }
}
