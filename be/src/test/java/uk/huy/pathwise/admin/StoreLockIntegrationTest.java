package uk.huy.pathwise.admin;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.Instant;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T56 acceptance (FR-A12, P-59, P-62): a READ_ONLY store can be read, every write of its members gets 423
 * {@code STORE_READ_ONLY} except the driver's results and location and the members' own accounts; unlocking makes it
 * writable again; lock and unlock are in the audit log.
 */
@AutoConfigureMockMvc
class StoreLockIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern INVITATION_CODE = Pattern.compile(">([A-Za-z0-9_-]{43})<");
    private static final String STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22}]";

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
    private TestStore store;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        admin = users.createAdmin();
        store = users.createStore();
        call(put("/api/v1/store"), store.owner(), "{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}").andExpect(status().isOk());
    }

    /**
     * Every group of store writes, by the member who may do it. Ids need not exist: the lock is checked first.
     */
    static Stream<Arguments> storeWrites() {
        return Stream.of(
                Arguments.of("PUT", "/api/v1/store", "owner", "{\"name\":\"X\",\"timezone\":\"Asia/Ho_Chi_Minh\"}"),
                Arguments.of("POST", "/api/v1/vehicles", "dispatcher", "{\"name\":\"Xe\",\"capacity\":1,\"capacityUnit\":\"BOX\"}"),
                Arguments.of("PUT", "/api/v1/vehicles/1", "dispatcher", "{}"),
                Arguments.of("POST", "/api/v1/plans", "dispatcher", "{}"),
                Arguments.of("PUT", "/api/v1/plans/1", "dispatcher", "{}"),
                Arguments.of("PATCH", "/api/v1/plans/1/stops/1", "dispatcher", "{}"),
                Arguments.of("POST", "/api/v1/plans/1/geocode", "dispatcher", "{}"),
                Arguments.of("POST", "/api/v1/plans/1/optimize", "dispatcher", "{\"version\":0}"),
                Arguments.of("PUT", "/api/v1/plans/1/routes", "dispatcher", "{}"),
                Arguments.of("POST", "/api/v1/plans/1/publish", "dispatcher", "{\"version\":0}"),
                Arguments.of("POST", "/api/v1/plans/1/cancel", "dispatcher", "{\"version\":0}"),
                Arguments.of("POST", "/api/v1/plans/1/copy", "dispatcher", "{\"planDate\":\"2026-12-01\",\"stops\":\"ALL\"}"),
                Arguments.of("PATCH", "/api/v1/store/members/1", "owner", "{\"canDrive\":true}"),
                Arguments.of("PATCH", "/api/v1/store/members/me/can-drive", "owner", "{\"canDrive\":true}"),
                Arguments.of("POST", "/api/v1/store/invitations", "owner", "{\"email\":\"x@pathwise.test\",\"role\":\"DRIVER\"}"),
                Arguments.of("POST", "/api/v1/store/invitations/1/revoke", "owner", null));
    }

    @ParameterizedTest(name = "{0} {1} by {2} -> 423")
    @MethodSource("storeWrites")
    void storeWrite_whenStoreLocked_423StoreReadOnly(String method, String path, String role, String body) throws Exception {
        lock("Chưa thanh toán");
        TestUser caller = role.equals("owner") ? store.owner() : store.dispatcher();

        call(request(HttpMethod.valueOf(method), path), caller, body)
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.code").value("STORE_READ_ONLY"));
    }

    @Test
    void lockedStore_whenReadOrDriverOrOwnAccount_stillWorks() throws Exception {
        long planId = publishedPlan();
        long routeId = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ?", Long.class, planId);
        long stopId = jdbcTemplate.queryForObject("SELECT id FROM route_plan_stops WHERE route_id = ? ORDER BY sequence LIMIT 1",
                Long.class, routeId);
        lock("  Chưa thanh toán  ");

        // Reads.
        call(get("/api/v1/store"), store.owner(), null).andExpect(status().isOk());
        call(get("/api/v1/vehicles"), store.dispatcher(), null).andExpect(status().isOk());
        call(get("/api/v1/plans/" + planId), store.dispatcher(), null).andExpect(status().isOk());
        // The banner: every member learns it from /users/me.
        for (TestUser member : List.of(store.owner(), store.dispatcher(), store.driver())) {
            call(get("/api/v1/users/me"), member, null)
                    .andExpect(jsonPath("$.data.storeStatus").value("READ_ONLY"))
                    .andExpect(jsonPath("$.data.storeLockReason").value("Chưa thanh toán"));
        }
        // The driver finishes the running deliveries.
        call(patch("/api/v1/driver/routes/" + routeId + "/stops/" + stopId + "/status"), store.driver(), "{\"status\":\"DELIVERED\"}")
                .andExpect(status().isOk());
        call(post("/api/v1/driver/location"), store.driver(), "{\"points\":[{\"lat\":16.06,\"lon\":108.21,\"recordedAt\":\""
                + Instant.now().minusSeconds(5) + "\"}]}").andExpect(status().is2xxSuccessful());
        // Own account.
        call(patch("/api/v1/users/me"), store.dispatcher(), "{\"fullName\":\"Điều phối\"}").andExpect(status().isOk());
        call(post("/api/v1/auth/sessions/revoke-others"), store.owner(), null).andExpect(status().is2xxSuccessful());
    }

    @Test
    void unlock_whenLocked_writesWorkAgainAndBothAreAudited() throws Exception {
        lock("Chưa thanh toán");
        call(post("/api/v1/vehicles"), store.dispatcher(), "{\"name\":\"Xe 1\",\"capacity\":5,\"capacityUnit\":\"BOX\"}").andExpect(status().is(423));

        call(post("/api/v1/admin/stores/" + store.id() + "/unlock"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.lockReason").value(nullValue()));
        call(post("/api/v1/vehicles"), store.dispatcher(), "{\"name\":\"Xe 1\",\"capacity\":5,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());
        call(get("/api/v1/users/me"), store.driver(), null)
                .andExpect(jsonPath("$.data.storeStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.data.storeLockReason").value(nullValue()));
        // Unlocking an ACTIVE store changes and records nothing.
        call(post("/api/v1/admin/stores/" + store.id() + "/unlock"), admin, null).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForList("SELECT action FROM audit_events WHERE store_id = ? AND action LIKE 'STORE_%LOCKED' ORDER BY id",
                String.class, store.id())).containsExactly("STORE_LOCKED", "STORE_UNLOCKED");
        assertThat(jdbcTemplate.queryForObject("SELECT details FROM audit_events WHERE store_id = ? AND action = 'STORE_UNLOCKED'",
                String.class, store.id())).contains("Chưa thanh toán");
        call(get("/api/v1/store/audit").param("action", "STORE_LOCKED"), store.owner(), null)
                .andExpect(jsonPath("$.data.items[0].details.reason").value("Chưa thanh toán"))
                .andExpect(jsonPath("$.data.items[0].ip").value(nullValue()));
    }

    @Test
    void acceptInvitation_whenStoreLocked_423AndCodeStillUsableAfterUnlock() throws Exception {
        String email = "moi-" + store.id() + "@pathwise.test";
        call(post("/api/v1/store/invitations"), store.owner(), "{\"email\":\"" + email + "\",\"role\":\"DRIVER\"}").andExpect(status().isCreated());
        Matcher code = INVITATION_CODE.matcher(emailSender.sentTo(email).getLast().content());
        assertThat(code.find()).isTrue();
        String accept = "{\"code\":\"" + code.group(1) + "\",\"password\":\"Passw0rd!x\",\"fullName\":\"Tài xế mới\"}";
        lock("Chưa thanh toán");

        mockMvc.perform(post("/api/v1/invitations/accept").contentType(MediaType.APPLICATION_JSON).content(accept))
                .andExpect(status().is(423))
                .andExpect(jsonPath("$.code").value("STORE_READ_ONLY"));

        call(post("/api/v1/admin/stores/" + store.id() + "/unlock"), admin, null).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/invitations/accept").contentType(MediaType.APPLICATION_JSON).content(accept))
                .andExpect(status().isNoContent());
    }

    @Test
    void lock_whenInvalidOrNotAdmin_rejected() throws Exception {
        call(post("/api/v1/admin/stores/" + store.id() + "/lock"), admin, "{\"reason\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("LOCK_REASON_REQUIRED"));
        call(post("/api/v1/admin/stores/" + store.id() + "/lock"), admin, "{\"reason\":\"" + "x".repeat(501) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("INVALID_LOCK_REASON_LENGTH"));
        call(post("/api/v1/admin/stores/999999999/lock"), admin, "{\"reason\":\"x\"}").andExpect(status().isNotFound());
        call(post("/api/v1/admin/stores/" + store.id() + "/lock"), store.owner(), "{\"reason\":\"x\"}").andExpect(status().isForbidden());
        // Locking again replaces the reason.
        lock("Lý do 1");
        call(post("/api/v1/admin/stores/" + store.id() + "/lock"), admin, "{\"reason\":\"Lý do 2\"}")
                .andExpect(jsonPath("$.data.lockReason").value("Lý do 2"));
        call(get("/api/v1/admin/stores/" + store.id()), admin, null)
                .andExpect(jsonPath("$.data.status").value("READ_ONLY"))
                .andExpect(jsonPath("$.data.lockReason").value("Lý do 2"))
                .andExpect(jsonPath("$.data.lockedAt").isNotEmpty());
    }

    private void lock(String reason) throws Exception {
        call(post("/api/v1/admin/stores/" + store.id() + "/lock"), admin, "{\"reason\":\"" + reason + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ_ONLY"))
                .andExpect(jsonPath("$.data.lockReason").value(reason.trim()));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, TestUser caller, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, caller.bearer());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private long publishedPlan() throws Exception {
        String vehicle = call(post("/api/v1/vehicles"), store.owner(), "{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long vehicleId = ((Number) JsonPath.read(vehicle, "$.data.id")).longValue();
        String body = call(post("/api/v1/plans"), store.dispatcher(), ("{\"name\":\"Giao sáng\",\"planDate\":\"2026-12-03\",\"stops\":%s,"
                + "\"assignments\":[{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                .formatted(STOPS, store.driver().id(), vehicleId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        call(post("/api/v1/plans/" + planId + "/optimize"), store.dispatcher(), "{\"version\":0}")
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        long version = jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
        call(post("/api/v1/plans/" + planId + "/publish"), store.dispatcher(), "{\"version\":" + version + "}").andExpect(status().isOk());
        return planId;
    }
}
