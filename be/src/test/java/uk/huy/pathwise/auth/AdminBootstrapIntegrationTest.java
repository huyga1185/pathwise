package uk.huy.pathwise.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.auth.infrastructure.config.BootstrapAdminProperties;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.service.AdminBootstrapService;
import uk.huy.pathwise.auth.service.AdminBootstrapService.Outcome;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T59 acceptance on the real database. Other tests of the shared database already created ADMINs, so the first run
 * sees "no ADMIN" through a stub; the second run uses the real check, as a restart would.
 */
@AutoConfigureMockMvc
class AdminBootstrapIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountService userAccountService;

    @Autowired
    private OpaqueTokenGenerator tokenGenerator;

    @Autowired
    private OtpManager otpManager;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void bootstrap_whenNoAdminYet_createOneVerifiedAdminThatLogsInAndNotAgainAfterRestart() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@pathwise.test";
        BootstrapAdminProperties properties = new BootstrapAdminProperties(email.toUpperCase(), "matkhau-admin-1", "Quản trị");

        assertThat(firstStart(properties).bootstrap()).isEqualTo(Outcome.CREATED);
        assertThat(restart(properties).bootstrap()).isEqualTo(Outcome.ADMIN_EXISTS);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ? AND role = 'ADMIN' AND status = 'ACTIVE' "
                        + "AND store_id IS NULL AND email_verified_at IS NOT NULL", Integer.class, email)).isEqualTo(1);
        logIn(email, "matkhau-admin-1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
        assertThat(emailSender.sentTo(email)).isEmpty();
    }

    @Test
    void bootstrap_whenNoPassword_emailResetOtpThatSetsThePassword() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@pathwise.test";

        assertThat(firstStart(new BootstrapAdminProperties(email, null, "Admin")).bootstrap()).isEqualTo(Outcome.CREATED);

        String otp = TestAccounts.lastOtp(emailSender, email);
        String body = mockMvc.perform(post("/api/v1/auth/password/reset/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String resetToken = com.jayway.jsonpath.JsonPath.read(body, "$.data.resetToken");
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"matkhau-moi-1\"}"))
                .andExpect(status().isNoContent());
        logIn(email, "matkhau-moi-1").andExpect(status().isOk());
    }

    @Test
    void bootstrap_whenEmailIsAnOwner_leaveTheOwnerAsIs() throws Exception {
        String email = "owner-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, "matkhau-owner-1");

        assertThat(firstStart(new BootstrapAdminProperties(email, "matkhau-admin-1", "Admin")).bootstrap())
                .isEqualTo(Outcome.EMAIL_TAKEN);

        assertThat(jdbcTemplate.queryForObject("SELECT role FROM users WHERE email = ?", String.class, email))
                .isEqualTo("OWNER");
    }

    /** A start on a database without ADMIN. */
    private AdminBootstrapService firstStart(BootstrapAdminProperties properties) {
        UserAccountService noAdminYet = mock(UserAccountService.class, delegatesTo(userAccountService));
        doReturn(false).when(noAdminYet).adminExists();
        return new AdminBootstrapService(properties, noAdminYet, tokenGenerator, otpManager);
    }

    private AdminBootstrapService restart(BootstrapAdminProperties properties) {
        return new AdminBootstrapService(properties, userAccountService, tokenGenerator, otpManager);
    }

    private ResultActions logIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }
}
