package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T15 acceptance: password change with the current password and an emailed OTP (D-17, P-05).
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
class ChangePasswordIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";
    private static final String NEW_PASSWORD = "N3wPassw0rd!";
    private static final String CHANGE_SUBJECT = "Mã xác nhận đổi mật khẩu";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void requestOtp_whenCurrentPasswordWrong_return400AndSendNoEmail() throws Exception {
        String email = registeredOwner();
        Session session = logIn(email, PASSWORD);

        requestOtp(session, "WrongPassw0rd")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        assertThat(changeEmails(email)).isZero();
    }

    @Test
    void changePassword_whenPasswordAndOtpCorrect_keepCurrentSessionAndRevokeOthers() throws Exception {
        String email = registeredOwner();
        Session current = logIn(email, PASSWORD);
        Session other = logIn(email, PASSWORD);

        requestOtp(current, PASSWORD).andExpect(status().isNoContent());
        assertThat(changeEmails(email)).isEqualTo(1);
        change(current, TestAccounts.lastOtp(emailSender, email), NEW_PASSWORD).andExpect(status().isNoContent());

        refresh(current.refreshToken()).andExpect(status().isOk());
        refresh(other.refreshToken()).andExpect(status().isUnauthorized());
        login(email, PASSWORD).andExpect(status().isUnauthorized());
        login(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void changePassword_whenOtpIssuedForAnotherUser_return400InvalidOtp() throws Exception {
        String emailA = registeredOwner();
        String emailB = registeredOwner();
        Session sessionA = logIn(emailA, PASSWORD);
        Session sessionB = logIn(emailB, PASSWORD);
        requestOtp(sessionA, PASSWORD).andExpect(status().isNoContent());

        change(sessionB, TestAccounts.lastOtp(emailSender, emailA), NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
        login(emailB, PASSWORD).andExpect(status().isOk());
        // User A's OTP is still valid for A.
        change(sessionA, TestAccounts.lastOtp(emailSender, emailA), NEW_PASSWORD).andExpect(status().isNoContent());
    }

    @Test
    void changePassword_whenOtpWrong_return400InvalidOtp() throws Exception {
        String email = registeredOwner();
        Session session = logIn(email, PASSWORD);
        requestOtp(session, PASSWORD).andExpect(status().isNoContent());
        String otp = TestAccounts.lastOtp(emailSender, email);

        change(session, otp.equals("000000") ? "111111" : "000000", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    @Test
    void requestOtp_whenAskedTwiceWithinCooldown_return429OtpCooldown() throws Exception {
        String email = registeredOwner();
        Session session = logIn(email, PASSWORD);

        requestOtp(session, PASSWORD).andExpect(status().isNoContent());
        requestOtp(session, PASSWORD)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
        assertThat(changeEmails(email)).isEqualTo(1);
    }

    @Test
    void passwordEndpoints_whenNoAccessToken_return401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password/change/otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/v1/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\",\"newPassword\":\"" + NEW_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void oldUpdatePasswordEndpoints_whenCalled_return404() throws Exception {
        Session session = logIn(registeredOwner(), PASSWORD);

        mockMvc.perform(get("/api/v1/auth/update-password").header(HttpHeaders.AUTHORIZATION, session.bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/auth/update-password/verify")
                        .header(HttpHeaders.AUTHORIZATION, session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"123456\"}"))
                .andExpect(status().isNotFound());
    }

    private String registeredOwner() throws Exception {
        String email = "t15-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        return email;
    }

    private long changeEmails(String email) {
        return emailSender.sentTo(email).stream().filter(sent -> sent.subject().startsWith(CHANGE_SUBJECT)).count();
    }

    private ResultActions requestOtp(Session session, String currentPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/change/otp")
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + currentPassword + "\"}"));
    }

    private ResultActions change(Session session, String otp, String newPassword) throws Exception {
        return mockMvc.perform(put("/api/v1/auth/password")
                .header(HttpHeaders.AUTHORIZATION, session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"otp\":\"" + otp + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private Session logIn(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken"));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + refreshToken + "\"}"));
    }

    private record Session(String accessToken, String refreshToken) {
        String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
