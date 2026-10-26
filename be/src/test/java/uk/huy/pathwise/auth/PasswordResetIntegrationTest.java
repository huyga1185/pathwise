package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T14 acceptance: forgotten password (P-06) against MariaDB and Redis.
 */
@AutoConfigureMockMvc
class PasswordResetIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";
    private static final String NEW_PASSWORD = "N3wPassw0rd!";
    private static final String OTHER_PASSWORD = "N3wPassw0rd!2";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void forgot_whenEmailUnknown_return204AndSendNothing() throws Exception {
        String email = newEmail();

        forgot(email).andExpect(status().isNoContent());

        assertThat(emailSender.sentTo(email)).isEmpty();
    }

    @Test
    void forgot_whenEmailNotVerified_return204AndSendNoResetOtp() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        forgot(email).andExpect(status().isNoContent());

        assertThat(emailSender.sentTo(email))
                .noneMatch(sent -> sent.subject().startsWith("Mã đặt lại mật khẩu"));
    }

    @Test
    void reset_whenFlowFollowed_changePasswordAndRevokeEverySession() throws Exception {
        String email = registeredOwner();
        String refreshA = logInMobile(email, PASSWORD);
        String refreshB = logInMobile(email, PASSWORD);

        forgot(email.toUpperCase()).andExpect(status().isNoContent());
        assertThat(emailSender.sentTo(email).getLast().subject()).startsWith("Mã đặt lại mật khẩu");
        Instant before = Instant.now();
        String body = verify(email, TestAccounts.lastOtp(emailSender, email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertThat(Instant.parse(JsonPath.read(body, "$.data.expiresAt")))
                .isBetween(before.plus(Duration.ofMinutes(10)).minusSeconds(5), Instant.now().plus(Duration.ofMinutes(10)));

        reset(JsonPath.read(body, "$.data.resetToken"), NEW_PASSWORD).andExpect(status().isNoContent());

        refresh(refreshA).andExpect(status().isUnauthorized());
        refresh(refreshB).andExpect(status().isUnauthorized());
        logIn(email, PASSWORD).andExpect(status().isUnauthorized());
        logIn(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void reset_whenTokenUsedTwice_return400InvalidResetToken() throws Exception {
        String email = registeredOwner();
        String resetToken = resetToken(email);
        reset(resetToken, NEW_PASSWORD).andExpect(status().isNoContent());

        reset(resetToken, OTHER_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        logIn(email, NEW_PASSWORD).andExpect(status().isOk());
    }

    @Test
    void reset_whenTokenExpired_return400InvalidResetToken() throws Exception {
        String email = registeredOwner();
        String resetToken = resetToken(email);
        String key = "otpToken:RESET_PASSWORD:" + resetToken.substring(0, resetToken.indexOf('.'));
        assertThat(redis.getExpire(key)).isBetween(590L, 600L);
        // Let Redis expire the key, as after 10 minutes.
        redis.expire(key, Duration.ofMillis(1));
        Thread.sleep(20);

        reset(resetToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        logIn(email, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void reset_whenTokenMalformedOrSecretWrong_return400InvalidResetToken() throws Exception {
        String email = registeredOwner();
        String resetToken = resetToken(email);
        String ticket = resetToken.substring(0, resetToken.indexOf('.'));

        reset("no-separator", NEW_PASSWORD).andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        reset(ticket + ".wrong-secret", NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
        // The first attempt consumed the token, even with a wrong secret.
        reset(resetToken, NEW_PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void verify_whenOtpWrongOrEmailUnknown_return400InvalidOtp() throws Exception {
        String email = registeredOwner();
        forgot(email).andExpect(status().isNoContent());
        String otp = TestAccounts.lastOtp(emailSender, email);

        verify(email, otp.equals("000000") ? "111111" : "000000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
        verify(newEmail(), "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    @Test
    void verify_whenOtpIssuedForEmailVerification_return400InvalidOtp() throws Exception {
        String email = newEmail();
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");

        verify(email, TestAccounts.lastOtp(emailSender, email))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    @Test
    void reset_whenNewPasswordTooShort_return400Validation() throws Exception {
        reset("ticket.secret", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("INVALID_PASSWORD_LENGTH"));
    }

    private String registeredOwner() throws Exception {
        String email = newEmail();
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        return email;
    }

    private String resetToken(String email) throws Exception {
        forgot(email).andExpect(status().isNoContent());
        String body = verify(email, TestAccounts.lastOtp(emailSender, email))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.resetToken");
    }

    private ResultActions forgot(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/forgot")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\"}"));
    }

    private ResultActions verify(String email, String otp) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/reset/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"otp\":\"" + otp + "\"}"));
    }

    private ResultActions reset(String resetToken, String newPassword) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/password/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"resetToken\":\"" + resetToken + "\",\"newPassword\":\"" + newPassword + "\"}"));
    }

    private ResultActions logIn(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/log-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String logInMobile(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
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

    private static String newEmail() {
        return "t14-" + UUID.randomUUID() + "@pathwise.test";
    }
}
