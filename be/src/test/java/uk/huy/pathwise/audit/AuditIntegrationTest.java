package uk.huy.pathwise.audit;

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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.audit.dto.AuditEntry;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.audit.service.AuditLog;
import uk.huy.pathwise.core.housekeeping.HousekeepingService;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T58 acceptance (FR-A14): each sensitive action writes exactly one audit row in the same transaction as the change;
 * the OWNER sees only its store, without the ADMIN's IP; the ADMIN sees everything.
 */
@AutoConfigureMockMvc
class AuditIntegrationTest extends AbstractIntegrationTest {
    private static final String STOPS = "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21},{\"label\":\"B\",\"lat\":16.07,\"lon\":108.22}]";
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private AuditLog auditLog;

    @Autowired
    private HousekeepingService housekeepingService;

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
    void updateMember_whenRoleAndCanDriveChange_recordOneEventWithTheChanges() throws Exception {
        call(patch("/api/v1/store/members/" + store.driver().id()), store.owner(), "{\"role\":\"DISPATCHER\",\"canDrive\":true}")
                .andExpect(status().isOk());
        // Same values again: nothing changed, nothing recorded.
        call(patch("/api/v1/store/members/" + store.driver().id()), store.owner(), "{\"role\":\"DISPATCHER\"}")
                .andExpect(status().isOk());

        List<Map<String, Object>> rows = rows(AuditAction.MEMBER_UPDATED);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.getFirst();
        assertThat(row).containsEntry("actor_id", store.owner().id()).containsEntry("actor_role", "OWNER")
                .containsEntry("actor_email", store.owner().email()).containsEntry("target_type", "USER")
                .containsEntry("target_id", store.driver().id()).containsEntry("ip", "127.0.0.1");
        assertThat(row.get("request_id")).isNotNull();
        assertThat(JsonPath.<String>read((String) row.get("details"), "$.email")).isEqualTo(store.driver().email());
        assertThat(JsonPath.<String>read((String) row.get("details"), "$.changes.role.from")).isEqualTo("DRIVER");
        assertThat(JsonPath.<String>read((String) row.get("details"), "$.changes.role.to")).isEqualTo("DISPATCHER");
        assertThat(JsonPath.<Boolean>read((String) row.get("details"), "$.changes.canDrive.to")).isTrue();
    }

    @Test
    void revokeInvitation_whenPending_recordEmailAndRole() throws Exception {
        String body = call(post("/api/v1/store/invitations"), store.owner(), "{\"email\":\"moi-" + store.id() + "@pathwise.test\",\"role\":\"DRIVER\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long invitationId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        call(post("/api/v1/store/invitations/" + invitationId + "/revoke"), store.owner(), null).andExpect(status().isNoContent());
        // Already revoked: 422, and no second row.
        call(post("/api/v1/store/invitations/" + invitationId + "/revoke"), store.owner(), null).andExpect(status().isUnprocessableEntity());

        List<Map<String, Object>> rows = rows(AuditAction.INVITATION_REVOKED);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row).containsEntry("target_type", "INVITATION").containsEntry("target_id", invitationId);
            assertThat(JsonPath.<String>read((String) row.get("details"), "$.role")).isEqualTo("DRIVER");
        });
    }

    @Test
    void planActions_whenEditedPublishedAndCancelled_recordEachOnceAndNothingForAFailure() throws Exception {
        long planId = optimizedPlan();
        long routeId = jdbcTemplate.queryForObject("SELECT id FROM route_plan_routes WHERE plan_id = ?", Long.class, planId);
        List<Long> stops = jdbcTemplate.queryForList("SELECT id FROM route_plan_stops WHERE plan_id = ? ORDER BY id", Long.class, planId);

        call(put("/api/v1/plans/" + planId + "/routes"), store.dispatcher(), "{\"version\":%d,\"routes\":[{\"routeId\":%d,\"stopIds\":[%d,%d]}],\"unassignedStopIds\":[]}"
                .formatted(version(planId), routeId, stops.get(1), stops.get(0))).andExpect(status().isOk());
        // A wrong version fails with 409 and leaves no row: the event is in the transaction of the change.
        call(post("/api/v1/plans/" + planId + "/publish"), store.dispatcher(), "{\"version\":999}").andExpect(status().isConflict());
        call(post("/api/v1/plans/" + planId + "/publish"), store.dispatcher(), "{\"version\":" + version(planId) + "}").andExpect(status().isOk());
        call(post("/api/v1/plans/" + planId + "/cancel"), store.dispatcher(), "{\"version\":" + version(planId) + "}").andExpect(status().isOk());

        assertThat(rows(AuditAction.PLAN_ROUTES_EDITED)).singleElement().satisfies(row ->
                assertThat(JsonPath.<Integer>read((String) row.get("details"), "$.violations")).isZero());
        assertThat(rows(AuditAction.PLAN_PUBLISHED)).singleElement().satisfies(row ->
                assertThat(row).containsEntry("target_id", planId).containsEntry("actor_role", "DISPATCHER"));
        assertThat(rows(AuditAction.PLAN_CANCELLED)).singleElement().satisfies(row ->
                assertThat(JsonPath.<String>read((String) row.get("details"), "$.from")).isEqualTo("PUBLISHED"));
    }

    @Test
    void storeAudit_whenOwnerReads_seeOnlyOwnStoreAndNoAdminIp() throws Exception {
        TestUser admin = users.createAdmin();
        TestStore other = users.createStore();
        call(patch("/api/v1/store/members/" + store.driver().id()), store.owner(), "{\"canDrive\":true}").andExpect(status().isOk());
        call(patch("/api/v1/store/members/" + other.driver().id()), other.owner(), "{\"canDrive\":true}").andExpect(status().isOk());
        call(get("/api/v1/admin/stores/" + store.id()), admin, null).andExpect(status().isOk());

        String today = LocalDate.now(VIETNAM).toString();
        call(get("/api/v1/store/audit").param("from", today).param("to", today), store.owner(), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                // Newest first: the ADMIN's view, without its IP.
                .andExpect(jsonPath("$.data.items[0].action").value("STORE_VIEWED"))
                .andExpect(jsonPath("$.data.items[0].actorRole").value("ADMIN"))
                .andExpect(jsonPath("$.data.items[0].ip").value(nullValue()))
                .andExpect(jsonPath("$.data.items[1].action").value("MEMBER_UPDATED"))
                .andExpect(jsonPath("$.data.items[1].ip").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.items[1].details.changes.canDrive.to").value(true));
        call(get("/api/v1/store/audit").param("action", "MEMBER_UPDATED"), store.owner(), null)
                .andExpect(jsonPath("$.data.totalElements").value(1));
        call(get("/api/v1/store/audit").param("from", LocalDate.now(VIETNAM).plusDays(1).toString()), store.owner(), null)
                .andExpect(jsonPath("$.data.totalElements").value(0));
        call(get("/api/v1/store/audit"), store.dispatcher(), null).andExpect(status().isForbidden());
        call(get("/api/v1/store/audit"), admin, null).andExpect(status().isForbidden());
    }

    @Test
    void adminAudit_whenFiltered_returnEveryStoreWithIp() throws Exception {
        TestUser admin = users.createAdmin();
        call(get("/api/v1/admin/stores/" + store.id()), admin, null).andExpect(status().isOk());
        // Not found: no row.
        call(get("/api/v1/admin/stores/999999999"), admin, null).andExpect(status().isNotFound());

        call(get("/api/v1/admin/audit").param("storeId", String.valueOf(store.id())).param("action", "STORE_VIEWED")
                        .param("actorId", String.valueOf(admin.id())), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].storeId").value(store.id()))
                .andExpect(jsonPath("$.data.items[0].targetType").value("STORE"))
                .andExpect(jsonPath("$.data.items[0].actorEmail").value(admin.email()))
                .andExpect(jsonPath("$.data.items[0].ip").value("127.0.0.1"))
                .andExpect(jsonPath("$.data.items[0].details").isMap());
        call(get("/api/v1/admin/audit").param("sort", "action"), admin, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
        call(get("/api/v1/admin/audit"), store.owner(), null).andExpect(status().isForbidden());
    }

    @Test
    void record_whenDetailsTooLarge_cutThemAndStillRecord() {
        UserIdentity owner = new UserIdentity(store.owner().id(), store.owner().email(), UserRole.OWNER, store.id(), false);

        auditLog.record(new AuditEntry(owner, store.id(), AuditAction.MEMBER_UPDATED, AuditTargetType.USER,
                store.driver().id(), Map.of("note", "\"x\"".repeat(3000))));

        String details = (String) rows(AuditAction.MEMBER_UPDATED).getFirst().get("details");
        assertThat(details.length()).isLessThanOrEqualTo(2000);
        assertThat(JsonPath.<String>read(details, "$.truncated")).startsWith("{\"note\":");
    }

    @Test
    void housekeeping_whenEventOlderThanAYear_deleteIt() {
        jdbcTemplate.update("INSERT INTO audit_events (occurred_at, store_id, action, target_type) VALUES "
                + "(UTC_TIMESTAMP(6) - INTERVAL 400 DAY, ?, 'PLAN_PUBLISHED', 'PLAN'), "
                + "(UTC_TIMESTAMP(6) - INTERVAL 300 DAY, ?, 'PLAN_CANCELLED', 'PLAN')", store.id(), store.id());

        assertThat(housekeepingService.run()).containsKey("audit-events");

        assertThat(rows(AuditAction.PLAN_PUBLISHED)).isEmpty();
        assertThat(rows(AuditAction.PLAN_CANCELLED)).hasSize(1);
    }

    private List<Map<String, Object>> rows(AuditAction action) {
        return jdbcTemplate.queryForList("SELECT * FROM audit_events WHERE store_id = ? AND action = ? ORDER BY id",
                store.id(), action.name());
    }

    private ResultActions call(MockHttpServletRequestBuilder request, TestUser caller, String body) throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, caller.bearer());
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private long optimizedPlan() throws Exception {
        String vehicle = call(post("/api/v1/vehicles"), store.owner(), "{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long vehicleId = ((Number) JsonPath.read(vehicle, "$.data.id")).longValue();
        String body = call(post("/api/v1/plans"), store.dispatcher(), ("{\"name\":\"Giao sáng\",\"planDate\":\"2026-12-02\",\"stops\":%s,"
                + "\"assignments\":[{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                .formatted(STOPS, store.driver().id(), vehicleId))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();
        call(post("/api/v1/plans/" + planId + "/optimize"), store.dispatcher(), "{\"version\":0}")
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
        return planId;
    }

    private long version(long planId) {
        return jdbcTemplate.queryForObject("SELECT version FROM route_plans WHERE id = ?", Long.class, planId);
    }
}
