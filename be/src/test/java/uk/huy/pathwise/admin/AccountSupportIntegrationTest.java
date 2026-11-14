package uk.huy.pathwise.admin;

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
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.FakeEmailSender.SentEmail;
import uk.huy.pathwise.support.TestAccounts;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T57 acceptance (FR-A13, P-63): the ADMIN resends a verification code, sends a password reset link that still needs
 * a code from the same email, and makes another member the OWNER. Every email goes to the right person; the ADMIN never
 * gets a code, a link or a password.
 */
@AutoConfigureMockMvc
class AccountSupportIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern LINK = Pattern.compile("/app/reset-password\\?token=([A-Za-z0-9_-]{43})\"");
    private static final String NEW_PASSWORD = "matkhau-moi-1";

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
    private TestUser admin;
    private TestStore store;

    @BeforeEach
    void setUp() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        admin = users.createAdmin();
        store = users.createStore();
    }

    @Test
    void verificationEmail_whenWaitingForVerification_sendNewCodeOnceThenCooldown() throws Exception {
        String email = "chua-xac-thuc-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.register(mockMvc, email, TestUsers.PASSWORD, "Tiệm chưa xác thực");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        // The code of the registration is a minute old.
        redisTemplate.delete("otp:cooldown:VERIFY_EMAIL:" + userId);

        asAdmin(post("/api/v1/admin/users/" + userId + "/verification-email")).andExpect(status().isNoContent());
        asAdmin(post("/api/v1/admin/users/" + userId + "/verification-email"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));

        assertThat(emailSender.sentTo(email)).hasSize(2).allSatisfy(sent -> assertThat(sent.subject()).startsWith("Mã xác thực email"));
        mockMvc.perform(post("/api/v1/auth/email/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + TestAccounts.lastOtp(emailSender, email) + "\"}"))
                .andExpect(status().is2xxSuccessful());
        asAdmin(post("/api/v1/admin/users/" + userId + "/verification-email"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_VERIFIED"));
        assertThat(audits("VERIFICATION_EMAIL_SENT", userId)).isEqualTo(1);
    }

    @Test
    void passwordResetLink_whenOpenedWithTheEmailCode_setPasswordAndEndSessions() throws Exception {
        TestUser member = store.dispatcher();
        String refreshToken = JsonPath.read(logIn(member.email(), TestUsers.PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.data.refreshToken");

        asAdmin(post("/api/v1/admin/users/" + member.id() + "/password-reset-email"))
                .andExpect(status().isNoContent())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());
        SentEmail linkEmail = emailSender.sentTo(member.email()).getLast();
        assertThat(linkEmail.subject()).startsWith("Đặt lại mật khẩu");
        String token = token(linkEmail);
        // The link does not carry the email, and lives 24 hours.
        assertThat(linkEmail.content()).doesNotContain("token=" + member.email());
        assertThat(redisTemplate.getExpire("resetLink:user:" + member.id(), TimeUnit.HOURS)).isBetween(23L, 24L);

        // The link alone is not enough: a wrong code fails and the link stays usable.
        resetByLink(token, "000000").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_OTP"));
        call(post("/api/v1/auth/password/reset-link/otp"), "{\"token\":\"" + token + "\"}").andExpect(status().isNoContent());
        String otp = TestAccounts.lastOtp(emailSender, member.email());
        resetByLink(token, otp).andExpect(status().isNoContent());

        logIn(member.email(), NEW_PASSWORD).andExpect(status().isOk());
        refresh(refreshToken).andExpect(status().isUnauthorized());
        // Used up.
        call(post("/api/v1/auth/password/reset-link/otp"), "{\"token\":\"" + token + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        assertThat(audits("PASSWORD_RESET_LINK_SENT", member.id())).isEqualTo(1);
    }

    @Test
    void passwordResetLink_whenSentAgainOrAccountNotActive_onlyTheLastLinkWorks() throws Exception {
        asAdmin(post("/api/v1/admin/users/" + store.driver().id() + "/password-reset-email")).andExpect(status().isNoContent());
        String first = token(emailSender.sentTo(store.driver().email()).getLast());
        asAdmin(post("/api/v1/admin/users/" + store.driver().id() + "/password-reset-email")).andExpect(status().isNoContent());
        String second = token(emailSender.sentTo(store.driver().email()).getLast());

        call(post("/api/v1/auth/password/reset-link/otp"), "{\"token\":\"" + first + "\"}")
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        call(post("/api/v1/auth/password/reset-link/otp"), "{\"token\":\"" + second + "\"}").andExpect(status().isNoContent());
        call(post("/api/v1/auth/password/reset-link/otp"), "{\"token\":\"khong-ton-tai\"}")
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));

        // Disabled after the link was sent: the link no longer works; and no new link for a disabled account.
        asAdmin(post("/api/v1/admin/users/" + store.driver().id() + "/disable")).andExpect(status().isOk());
        resetByLink(second, "123456").andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        asAdmin(post("/api/v1/admin/users/" + store.driver().id() + "/password-reset-email"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        asAdmin(post("/api/v1/admin/users/999999999/password-reset-email")).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/admin/users/" + store.dispatcher().id() + "/password-reset-email")
                .header(HttpHeaders.AUTHORIZATION, store.owner().bearer())).andExpect(status().isForbidden());
    }

    @Test
    void transferOwner_whenDriverChosen_rolesSwapOldOwnerLoggedOutAndBothEmailed() throws Exception {
        TestUser oldOwner = store.owner();
        TestUser driver = store.driver();
        String oldRefresh = JsonPath.read(logIn(oldOwner.email(), TestUsers.PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.data.refreshToken");
        String driverRefresh = JsonPath.read(logIn(driver.email(), TestUsers.PASSWORD).andReturn().getResponse().getContentAsString(),
                "$.data.refreshToken");

        asAdmin(post("/api/v1/admin/stores/" + store.id() + "/owner"), "{\"userId\":" + driver.id() + "}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ownerId").value(driver.id()))
                .andExpect(jsonPath("$.data.ownerCanDrive").value(true))
                .andExpect(jsonPath("$.data.previousOwnerId").value(oldOwner.id()))
                .andExpect(jsonPath("$.data.previousOwnerEmail").value(oldOwner.email()));

        assertThat(jdbcTemplate.queryForList("SELECT role FROM users WHERE store_id = ? ORDER BY id", String.class, store.id()))
                .containsExactly("DISPATCHER", "DISPATCHER", "OWNER");
        // The old OWNER is logged out, and logs in again as DISPATCHER; the new one refreshes into OWNER.
        refresh(oldRefresh).andExpect(status().isUnauthorized());
        logIn(oldOwner.email(), TestUsers.PASSWORD).andExpect(jsonPath("$.data.user.role").value("DISPATCHER"));
        String access = JsonPath.read(refresh(driverRefresh).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.data.accessToken");
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.canDrive").value(true));

        SentEmail granted = emailSender.sentTo(driver.email()).getLast();
        SentEmail transferred = emailSender.sentTo(oldOwner.email()).getLast();
        assertThat(granted.subject()).startsWith("Bạn đã trở thành chủ cửa hàng");
        assertThat(transferred.subject()).startsWith("Quyền chủ cửa hàng đã được chuyển");
        assertThat(transferred.content()).doesNotContain(driver.email());
        assertThat(jdbcTemplate.queryForObject("SELECT details FROM audit_events WHERE action = 'STORE_OWNER_TRANSFERRED' "
                + "AND store_id = ?", String.class, store.id())).contains(oldOwner.email(), driver.email());
    }

    @Test
    void transferOwner_whenMemberNotEligible_rejectedAndNothingChanges() throws Exception {
        TestStore other = users.createStore();
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", store.driver().id());

        asAdmin(post("/api/v1/admin/stores/" + store.id() + "/owner"), "{\"userId\":" + other.dispatcher().id() + "}")
                .andExpect(status().isNotFound());
        asAdmin(post("/api/v1/admin/stores/999999999/owner"), "{\"userId\":" + store.dispatcher().id() + "}")
                .andExpect(status().isNotFound());
        asAdmin(post("/api/v1/admin/stores/" + store.id() + "/owner"), "{\"userId\":" + store.driver().id() + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("userId"))
                .andExpect(jsonPath("$.details[0].code").value("NEW_OWNER_NOT_ACTIVE"));
        asAdmin(post("/api/v1/admin/stores/" + store.id() + "/owner"), "{\"userId\":" + store.owner().id() + "}")
                .andExpect(jsonPath("$.details[0].code").value("ALREADY_OWNER"));
        asAdmin(post("/api/v1/admin/stores/" + store.id() + "/owner"), "{}")
                .andExpect(jsonPath("$.details[0].code").value("USER_ID_REQUIRED"));
        mockMvc.perform(post("/api/v1/admin/stores/" + store.id() + "/owner").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":" + store.dispatcher().id() + "}"))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT role FROM users WHERE id = ?", String.class, store.owner().id())).isEqualTo("OWNER");
    }

    private int audits(String action, long targetId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_events WHERE action = ? AND target_id = ? AND actor_id = ?",
                Integer.class, action, targetId, admin.id());
    }

    private static String token(SentEmail email) {
        Matcher matcher = LINK.matcher(email.content());
        assertThat(matcher.find()).as("link in the email").isTrue();
        return matcher.group(1);
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, admin.bearer()));
    }

    private ResultActions asAdmin(MockHttpServletRequestBuilder request, String body) throws Exception {
        return asAdmin(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions resetByLink(String token, String otp) throws Exception {
        return call(post("/api/v1/auth/password/reset-link"),
                "{\"token\":\"" + token + "\",\"otp\":\"" + otp + "\",\"newPassword\":\"" + NEW_PASSWORD + "\"}");
    }

    private ResultActions logIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in").header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }
}
