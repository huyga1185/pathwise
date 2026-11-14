package uk.huy.pathwise.admin;

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
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T56 acceptance (FR-A12): the ADMIN disables and enables accounts and ends their sessions; each action is audited
 * with the user's store.
 */
@AutoConfigureMockMvc
class AdminUserActionIntegrationTest extends AbstractIntegrationTest {
    private static final String USERS = "/api/v1/admin/users/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUser admin;
    private TestStore store;

    @BeforeEach
    void setUp() throws Exception {
        TestUsers users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        admin = users.createAdmin();
        store = users.createStore();
    }

    @Test
    void disable_whenMember_cannotRefreshNorLogInUntilEnabled() throws Exception {
        String refreshToken = logIn(store.dispatcher().email()).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        refreshToken = JsonPath.read(refreshToken, "$.data.refreshToken");

        asAdmin(post(USERS + store.dispatcher().id() + "/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISABLED"))
                .andExpect(jsonPath("$.data.storeId").value(store.id()));

        refresh(refreshToken).andExpect(status().isUnauthorized());
        logIn(store.dispatcher().email()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));

        asAdmin(post(USERS + store.dispatcher().id() + "/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
        logIn(store.dispatcher().email()).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForList("SELECT action FROM audit_events WHERE target_type = 'USER' AND target_id = ? "
                + "AND store_id = ? ORDER BY id", String.class, store.dispatcher().id(), store.id()))
                .containsExactly("USER_DISABLED", "USER_ENABLED");
        mockMvc.perform(get("/api/v1/store/audit").param("action", "USER_DISABLED")
                        .header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.items[0].details.email").value(store.dispatcher().email()));
    }

    @Test
    void enable_whenEmailNeverVerified_backToPendingVerification() throws Exception {
        jdbcTemplate.update("UPDATE users SET email_verified_at = NULL, status = 'DISABLED' WHERE id = ?", store.driver().id());

        asAdmin(post(USERS + store.driver().id() + "/enable"))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));
    }

    @Test
    void revokeSessions_whenUserLoggedIn_refreshFailsButLogInWorks() throws Exception {
        String body = logIn(store.owner().email()).andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(body, "$.data.refreshToken");

        asAdmin(post(USERS + store.owner().id() + "/sessions/revoke")).andExpect(status().isNoContent());

        refresh(refreshToken).andExpect(status().isUnauthorized());
        logIn(store.owner().email()).andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action = 'USER_SESSIONS_REVOKED' "
                + "AND target_id = ? AND actor_id = ?", Integer.class, store.owner().id(), admin.id())).isEqualTo(1);
    }

    @Test
    void userActions_whenSelfUnknownOrNotAdmin_rejected() throws Exception {
        asAdmin(post(USERS + admin.id() + "/disable"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CANNOT_MODIFY_SELF"));
        asAdmin(post(USERS + "999999999/disable")).andExpect(status().isNotFound());
        asAdmin(post(USERS + "999999999/enable")).andExpect(status().isNotFound());
        asAdmin(post(USERS + "999999999/sessions/revoke")).andExpect(status().isNotFound());
        for (TestUser member : java.util.List.of(store.owner(), store.dispatcher(), store.driver())) {
            mockMvc.perform(post(USERS + store.driver().id() + "/disable").header(HttpHeaders.AUTHORIZATION, member.bearer()))
                    .andExpect(status().isForbidden());
        }
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM users WHERE id = ?", String.class, store.driver().id()))
                .isEqualTo("ACTIVE");
    }

    private ResultActions asAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin.bearer()));
    }

    private ResultActions logIn(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in").header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }
}
