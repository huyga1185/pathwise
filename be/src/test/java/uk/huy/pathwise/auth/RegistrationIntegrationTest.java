package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T13 acceptance: OWNER registration with its store, email verification, and log-in/refresh by account status.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
class RegistrationIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void register_whenValid_createPendingOwnerWithStoreAndEmailOtp() throws Exception {
        String email = newEmail();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD
                                + "\",\"storeName\":\" Tiệm An \",\"fullName\":\"An\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT u.role, u.status, u.full_name, u.email_verified_at, u.can_drive, s.name, s.timezone "
                        + "FROM users u JOIN stores s ON s.id = u.store_id WHERE u.email = ?", email);
        assertThat(user).containsEntry("role", "OWNER")
                .containsEntry("status", "PENDING_VERIFICATION")
                .containsEntry("full_name", "An")
                .containsEntry("email_verified_at", null)
                .containsEntry("can_drive", false)
                .containsEntry("name", "Tiệm An")
                .containsEntry("timezone", "Asia/Ho_Chi_Minh");
        assertThat(emailSender.sentTo(email)).singleElement()
                .satisfies(sent -> assertThat(sent.subject()).startsWith("Mã xác thực email"));
        assertThat(TestAccounts.lastOtp(emailSender, email)).hasSize(6);
    }

    @Test
    void logIn_whenEmailNotVerified_return403EmailNotVerified() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        logIn(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void logIn_whenEmailNotVerifiedButPasswordWrong_return401NotRevealingStatus() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        logIn(email, "WrongPassw0rd")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_OR_PASSWORD"));
    }

    @Test
    void verifyEmail_whenCorrectOtp_activateAccountAndLogInWorks() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        verifyEmail(email, TestAccounts.lastOtp(emailSender, email)).andExpect(status().isNoContent());

        Map<String, Object> user = jdbcTemplate.queryForMap(
                "SELECT id, status, store_id, email_verified_at FROM users WHERE email = ?", email);
        assertThat(user.get("status")).isEqualTo("ACTIVE");
        assertThat(user.get("email_verified_at")).isNotNull();
        String body = logIn(email.toUpperCase(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("OWNER"))
                .andExpect(jsonPath("$.data.user.storeId").value(((Number) user.get("store_id")).longValue()))
                .andExpect(jsonPath("$.data.user.canDrive").value(false))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(body, "$.data.accessToken")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.storeId").value(((Number) user.get("store_id")).longValue()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.canDrive").value(false));
    }

    @Test
    void verifyEmail_whenWrongOtp_return400InvalidOtpAndKeepPending() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");
        String otp = TestAccounts.lastOtp(emailSender, email);

        verifyEmail(email, otp.equals("000000") ? "111111" : "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
        assertThat(accountStatus(email)).isEqualTo("PENDING_VERIFICATION");
    }

    @Test
    void verifyEmail_whenUnknownEmail_return400InvalidOtp() throws Exception {
        verifyEmail(newEmail(), "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    @Test
    void verifyEmail_whenAlreadyVerified_return409EmailAlreadyVerified() throws Exception {
        String email = newEmail();
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);

        verifyEmail(email, "123456")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_VERIFIED"));
    }

    @Test
    void register_whenEmailExistsInAnotherCase_return409AndCreateNoStore() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");
        Integer storesBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Integer.class);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD
                                + "\",\"storeName\":\"Other\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTED"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stores", Integer.class)).isEqualTo(storesBefore);
    }

    @Test
    void register_whenEmailCannotBeSent_return201AndResendWorksRightAway() throws Exception {
        String email = newEmail();
        emailSender.failNextSend();

        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");
        assertThat(emailSender.sentTo(email)).isEmpty();
        assertThat(accountStatus(email)).isEqualTo("PENDING_VERIFICATION");

        resend(email).andExpect(status().isNoContent());
        verifyEmail(email, TestAccounts.lastOtp(emailSender, email)).andExpect(status().isNoContent());
    }

    @Test
    void resend_whenCooldownRunning_return204AndSendNothing() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        resend(email).andExpect(status().isNoContent());

        assertThat(emailSender.sentTo(email)).hasSize(1);
    }

    @Test
    void resend_whenCooldownOver_sendNewOtpAndOldOneStopsWorking() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");
        String first = TestAccounts.lastOtp(emailSender, email);
        redis.delete("otp:cooldown:VERIFY_EMAIL:" + userId(email));

        resend(email).andExpect(status().isNoContent());

        assertThat(emailSender.sentTo(email)).hasSize(2);
        String second = TestAccounts.lastOtp(emailSender, email);
        if (!first.equals(second)) verifyEmail(email, first).andExpect(status().isBadRequest());
        verifyEmail(email, second).andExpect(status().isNoContent());
    }

    @Test
    void resend_whenUnknownOrVerifiedEmail_return204AndSendNothing() throws Exception {
        String unknown = newEmail();
        String verified = newEmail();
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, verified, PASSWORD);
        redis.delete("otp:cooldown:VERIFY_EMAIL:" + userId(verified));

        resend(unknown).andExpect(status().isNoContent());
        resend(verified).andExpect(status().isNoContent());

        assertThat(emailSender.sentTo(unknown)).isEmpty();
        assertThat(emailSender.sentTo(verified)).hasSize(1);
    }

    @Test
    void logIn_whenAccountDisabled_return403AccountDisabled() throws Exception {
        String email = newEmail();
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE email = ?", email);

        logIn(email, PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void refresh_whenAccountDisabledAfterLogIn_return401InvalidToken() throws Exception {
        String email = newEmail();
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE email = ?", email);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + JsonPath.read(body, "$.data.refreshToken") + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void flyway_whenStartedOnEmptyDatabase_appliedV1AndV2() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank", String.class))
                .containsSubsequence("1", "2");
    }

    private ResultActions logIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, password)));
    }

    private ResultActions verifyEmail(String email, String otp) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"));
    }

    private ResultActions resend(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/email/verify/resend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private String accountStatus(String email) {
        return jdbcTemplate.queryForObject("SELECT status FROM users WHERE email = ?", String.class, email);
    }

    private long userId(String email) {
        return jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
    }

    private static String newEmail() {
        return "t13-" + UUID.randomUUID() + "@pathwise.test";
    }

    private static String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
