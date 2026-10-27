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
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T16 acceptance: invitations of DISPATCHERs and DRIVERs (D-16, D-27).
 */
@AutoConfigureMockMvc
class StoreInvitationIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern CODE_IN_EMAIL = Pattern.compile(">([A-Za-z0-9_-]{43})<");
    private static final String PASSWORD = "Passw0rd!x";

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
    void invite_whenOwner_return201AndEmailCode() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        Instant before = Instant.now();

        String body = invite(store.owner().bearer(), email.toUpperCase(), "DRIVER")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("DRIVER"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        assertThat(Instant.parse(JsonPath.read(body, "$.data.expiresAt")))
                .isBetween(before.plus(Duration.ofDays(7)).minusSeconds(5), Instant.now().plus(Duration.ofDays(7)));
        FakeEmailSender.SentEmail sent = emailSender.sentTo(email).getLast();
        assertThat(sent.subject()).startsWith("Lời mời tham gia cửa hàng");
        assertThat(sent.content()).contains("Cửa hàng " + store.owner().email()).contains("tài xế");
        assertThat(code(email)).hasSize(43);
    }

    @Test
    void accept_whenCodeValid_createActiveMemberWhoCanLogInWithRoleAndStore() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        invite(store.owner().bearer(), email, "DRIVER").andExpect(status().isCreated());
        String code = code(email);

        mockMvc.perform(get("/api/v1/invitations/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("Cửa hàng " + store.owner().email()))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("DRIVER"));
        accept(code, "Tùng", "0905123456").andExpect(status().isNoContent());

        logIn(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("DRIVER"))
                .andExpect(jsonPath("$.data.user.storeId").value(store.id()))
                .andExpect(jsonPath("$.data.user.fullName").value("Tùng"))
                .andExpect(jsonPath("$.data.user.canDrive").value(true));
        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT status, email_verified_at, phone_number FROM users WHERE email = ?", email);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("email_verified_at")).isNotNull();
        assertThat(user.get("phone_number")).isEqualTo("+84905123456");
        listInvitations(store.owner().bearer(), "")
                .andExpect(jsonPath("$.data[?(@.email == '" + email + "')].status").value("ACCEPTED"));
    }

    @Test
    void accept_whenCodeAlreadyUsed_return404InvitationInvalid() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        invite(store.owner().bearer(), email, "DISPATCHER").andExpect(status().isCreated());
        String code = code(email);
        accept(code, "An", null).andExpect(status().isNoContent());

        accept(code, "An", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
        mockMvc.perform(get("/api/v1/invitations/" + code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
    }

    @Test
    void accept_whenInvitationRevoked_return404InvitationInvalid() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        long id = invitationId(invite(store.owner().bearer(), email, "DRIVER"));
        String code = code(email);

        mockMvc.perform(delete("/api/v1/store/invitations/" + id).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isNoContent());

        accept(code, "An", null).andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email)).isZero();
    }

    @Test
    void accept_whenInvitationExpired_return404AndListShowsExpired() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        long id = invitationId(invite(store.owner().bearer(), email, "DRIVER"));
        jdbcTemplate.update("UPDATE store_invitations SET expires_at = UTC_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE id = ?", id);

        accept(code(email), "An", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
        listInvitations(store.owner().bearer(), "?status=EXPIRED")
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.data[0].status").value("EXPIRED"));
        mockMvc.perform(delete("/api/v1/store/invitations/" + id).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_PENDING"));
    }

    @Test
    void invite_whenEmailAlreadyHasAccount_return409EmailExisted() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();

        invite(storeA.owner().bearer(), storeB.driver().email(), "DRIVER")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTED"));
        assertThat(emailSender.sentTo(storeB.driver().email())).isEmpty();
    }

    @Test
    void invite_whenSameEmailInvitedAgain_revokePreviousInvitation() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        long first = invitationId(invite(store.owner().bearer(), email, "DRIVER"));
        String firstCode = code(email);

        invite(store.owner().bearer(), email, "DISPATCHER").andExpect(status().isCreated());

        accept(firstCode, "An", null).andExpect(jsonPath("$.code").value("INVITATION_INVALID"));
        listInvitations(store.owner().bearer(), "?status=REVOKED").andExpect(jsonPath("$.data[0].id").value(first));
        accept(code(email), "An", null).andExpect(status().isNoContent());
        logIn(email).andExpect(jsonPath("$.data.user.role").value("DISPATCHER"));
    }

    @Test
    void accept_whenPhoneNumberInvalid_return400AndKeepInvitationPending() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        invite(store.owner().bearer(), email, "DRIVER").andExpect(status().isCreated());
        String code = code(email);

        accept(code, "An", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PHONE_NUMBER"));
        accept(code, "An", null).andExpect(status().isNoContent());
    }

    @Test
    void invite_whenDispatcherOrDriver_return403() throws Exception {
        TestStore store = users.createStore();

        invite(store.dispatcher().bearer(), newEmail(), "DRIVER")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        invite(store.driver().bearer(), newEmail(), "DRIVER").andExpect(status().isForbidden());
        listInvitations(store.dispatcher().bearer(), "").andExpect(status().isForbidden());
    }

    @Test
    void invite_whenRoleIsOwner_return400() throws Exception {
        TestStore store = users.createStore();

        invite(store.owner().bearer(), newEmail(), "OWNER")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void revoke_whenInvitationOfAnotherStore_return404AndKeepIt() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();
        String email = newEmail();
        long id = invitationId(invite(storeA.owner().bearer(), email, "DRIVER"));

        mockMvc.perform(delete("/api/v1/store/invitations/" + id).header(HttpHeaders.AUTHORIZATION, storeB.owner().bearer()))
                .andExpect(status().isNotFound());

        listInvitations(storeB.owner().bearer(), "").andExpect(jsonPath("$.data").isEmpty());
        listInvitations(storeA.owner().bearer(), "").andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void invite_whenEmailCannotBeSent_return201AndInvitationStaysPending() throws Exception {
        TestStore store = users.createStore();
        String email = newEmail();
        emailSender.failNextSend();

        invite(store.owner().bearer(), email, "DRIVER").andExpect(status().isCreated());

        assertThat(emailSender.sentTo(email)).isEmpty();
        listInvitations(store.owner().bearer(), "?status=PENDING").andExpect(jsonPath("$.data[0].email").value(email));
    }

    private ResultActions invite(String bearer, String email, String role) throws Exception {
        return mockMvc.perform(post("/api/v1/store/invitations")
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}"));
    }

    private ResultActions listInvitations(String bearer, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/store/invitations" + query).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions accept(String code, String fullName, String phoneNumber) throws Exception {
        String phone = phoneNumber == null ? "" : ",\"phoneNumber\":\"" + phoneNumber + "\"";
        return mockMvc.perform(post("/api/v1/invitations/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"" + fullName + "\"" + phone + "}"));
    }

    private ResultActions logIn(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"));
    }

    private static long invitationId(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private String code(String email) {
        Matcher code = CODE_IN_EMAIL.matcher(emailSender.sentTo(email).getLast().content());
        assertThat(code.find()).isTrue();
        return code.group(1);
    }

    private static String newEmail() {
        return "t16-" + UUID.randomUUID() + "@pathwise.test";
    }
}
