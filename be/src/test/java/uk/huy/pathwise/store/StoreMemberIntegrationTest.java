package uk.huy.pathwise.store;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T16: member management (FR-S04, FR-S05, FR-S06) and its tenant isolation.
 */
@AutoConfigureMockMvc
class StoreMemberIntegrationTest extends AbstractIntegrationTest {
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
    void listMembers_whenOwnerOrDispatcher_returnOwnStoreMembersOnly() throws Exception {
        TestStore store = users.createStore();
        users.createStore();

        for (TestUser caller : new TestUser[]{store.owner(), store.dispatcher()}) {
            members(caller, "")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[*].email", containsInAnyOrder(
                            store.owner().email(), store.dispatcher().email(), store.driver().email())));
        }
        members(store.owner(), "?role=DRIVER")
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].canDrive").value(true));
    }

    @Test
    void listMembers_whenAssignable_returnActiveDriversAndMembersWithCanDrive() throws Exception {
        TestStore store = users.createStore();
        users.withCanDrive(store.owner(), true);
        TestUser disabledDriver = users.addMember(store.id(), UserRole.DRIVER, false);
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", disabledDriver.id());

        members(store.dispatcher(), "?assignable=true")
                .andExpect(jsonPath("$.data[*].email", containsInAnyOrder(store.owner().email(), store.driver().email())));
    }

    @Test
    void listMembers_whenDriver_return403() throws Exception {
        TestStore store = users.createStore();

        members(store.driver(), "").andExpect(status().isForbidden());
    }

    @Test
    void updateMember_whenDisabled_revokeSessionsAndBlockLogIn() throws Exception {
        TestStore store = users.createStore();
        String refreshToken = mobileRefreshToken(store.dispatcher().email());

        updateMember(store.owner(), store.dispatcher().id(), "{\"status\":\"DISABLED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        assertThat(jdbcTemplate.queryForList("SELECT state FROM refresh_tokens WHERE user_id = ?", String.class,
                store.dispatcher().id())).isNotEmpty().allMatch("REVOKED"::equals);
        refresh(refreshToken).andExpect(status().isUnauthorized());
        logIn(store.dispatcher().email())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        updateMember(store.owner(), store.dispatcher().id(), "{\"status\":\"ACTIVE\"}").andExpect(status().isOk());
        logIn(store.dispatcher().email()).andExpect(status().isOk());
    }

    @Test
    void updateMember_whenRoleAndCanDriveChanged_returnUpdatedMember() throws Exception {
        TestStore store = users.createStore();

        updateMember(store.owner(), store.driver().id(), "{\"role\":\"DISPATCHER\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("DISPATCHER"))
                .andExpect(jsonPath("$.data.canDrive").value(false));
        updateMember(store.owner(), store.driver().id(), "{\"canDrive\":true}")
                .andExpect(jsonPath("$.data.role").value("DISPATCHER"))
                .andExpect(jsonPath("$.data.canDrive").value(true));
        logIn(store.driver().email()).andExpect(jsonPath("$.data.user.role").value("DISPATCHER"));
    }

    @Test
    void updateMember_whenFieldIsNull_return400NotNullableAndChangeNothing() throws Exception {
        TestStore store = users.createStore();

        updateMember(store.owner(), store.driver().id(), "{\"status\":null,\"canDrive\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'status')].code").value("NOT_NULLABLE"))
                .andExpect(jsonPath("$.details[?(@.field == 'canDrive')].code").value("NOT_NULLABLE"));
        updateMember(store.owner(), store.driver().id(), "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.role").value("DRIVER"));
    }

    @Test
    void listMembers_whenPhoneNumberSet_returnE164() throws Exception {
        TestStore store = users.createStore();
        jdbcTemplate.update("UPDATE users SET phone_number = '+84905123456' WHERE id = ?", store.driver().id());

        members(store.owner(), "?role=DRIVER").andExpect(jsonPath("$.data[0].phoneNumber").value("+84905123456"));
    }

    @Test
    void updateMember_whenTargetIsOwner_return422CannotModifyOwner() throws Exception {
        TestStore store = users.createStore();

        updateMember(store.owner(), store.owner().id(), "{\"status\":\"DISABLED\"}")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_OWNER"));
        updateMember(store.owner(), store.owner().id(), "{\"role\":\"DRIVER\"}")
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_OWNER"));
        logIn(store.owner().email()).andExpect(jsonPath("$.data.user.role").value("OWNER"));
    }

    @Test
    void updateMember_whenUserOfAnotherStore_return404AndChangeNothing() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();

        updateMember(storeB.owner(), storeA.dispatcher().id(), "{\"status\":\"DISABLED\"}")
                .andExpect(status().isNotFound());

        logIn(storeA.dispatcher().email()).andExpect(status().isOk());
    }

    @Test
    void updateMember_whenDispatcher_return403() throws Exception {
        TestStore store = users.createStore();

        updateMember(store.dispatcher(), store.driver().id(), "{\"status\":\"DISABLED\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOwnCanDrive_whenOwner_turnFlagOnAndOff() throws Exception {
        TestStore store = users.createStore();

        canDrive(store.owner(), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(store.owner().id()))
                .andExpect(jsonPath("$.data.canDrive").value(true));
        logIn(store.owner().email()).andExpect(jsonPath("$.data.user.canDrive").value(true));
        canDrive(store.owner(), false).andExpect(jsonPath("$.data.canDrive").value(false));
    }

    @Test
    void updateOwnCanDrive_whenDispatcher_return403() throws Exception {
        TestStore store = users.createStore();

        canDrive(store.dispatcher(), true).andExpect(status().isForbidden());
    }

    private ResultActions members(TestUser caller, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/store/members" + query).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private ResultActions updateMember(TestUser caller, long userId, String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/store/members/" + userId)
                .header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions canDrive(TestUser caller, boolean canDrive) throws Exception {
        return mockMvc.perform(patch("/api/v1/store/members/me/can-drive")
                .header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"canDrive\":" + canDrive + "}"));
    }

    private ResultActions logIn(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}"));
    }

    private String mobileRefreshToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.refreshToken");
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }
}
