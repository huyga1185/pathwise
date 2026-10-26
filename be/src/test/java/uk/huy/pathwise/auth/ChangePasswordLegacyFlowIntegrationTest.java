package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The existing change-password flow (GET OTP → verify → update) must keep working on the new OtpManager (T12);
 * T15 replaces it.
 */
@AutoConfigureMockMvc
class ChangePasswordLegacyFlowIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern OTP_IN_EMAIL = Pattern.compile(">(\\d{6})<");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void changePassword_whenOtpFromEmailIsUsed_newPasswordWorks() throws Exception {
        String email = "t12-" + UUID.randomUUID() + "@pathwise.test";
        register(email, "Passw0rd!x");
        String accessToken = accessToken(email, "Passw0rd!x");

        mockMvc.perform(get("/api/v1/auth/update-password").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        FakeEmailSender.SentEmail sent = emailSender.sentTo(email).getLast();
        assertThat(sent.subject()).isEqualTo("Mã xác nhận đổi mật khẩu - PathWise");
        Matcher otp = OTP_IN_EMAIL.matcher(sent.content());
        assertThat(otp.find()).isTrue();

        MockHttpServletResponse verified = mockMvc.perform(post("/api/v1/auth/update-password/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + otp.group(1) + "\"}"))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();
        Cookie token = verified.getCookie("updateUserPassword");
        Cookie ticket = verified.getCookie("updateUserPasswordTicket");
        assertThat(token).isNotNull();
        assertThat(ticket).isNotNull();

        mockMvc.perform(post("/api/v1/auth/update-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .cookie(new Cookie(token.getName(), token.getValue()), new Cookie(ticket.getName(), ticket.getValue()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"N3wPassw0rd!\"}"))
                .andExpect(status().isNoContent());

        accessToken(email, "N3wPassw0rd!");
    }

    @Test
    void requestOtp_whenAskedTwiceWithinCooldown_return429OtpCooldown() throws Exception {
        String email = "t12-" + UUID.randomUUID() + "@pathwise.test";
        register(email, "Passw0rd!x");
        String accessToken = accessToken(email, "Passw0rd!x");

        mockMvc.perform(get("/api/v1/auth/update-password").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/auth/update-password").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("OTP_COOLDOWN"));
        assertThat(emailSender.sentTo(email))
                .filteredOn(sent -> sent.subject().startsWith("Mã xác nhận đổi mật khẩu"))
                .hasSize(1);
    }

    @Test
    void verifyOtp_whenWrongCode_return400InvalidOtp() throws Exception {
        String email = "t12-" + UUID.randomUUID() + "@pathwise.test";
        register(email, "Passw0rd!x");
        String accessToken = accessToken(email, "Passw0rd!x");
        mockMvc.perform(get("/api/v1/auth/update-password").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNoContent());
        Matcher otp = OTP_IN_EMAIL.matcher(emailSender.sentTo(email).getLast().content());
        assertThat(otp.find()).isTrue();
        String wrong = otp.group(1).equals("000000") ? "111111" : "000000";

        mockMvc.perform(post("/api/v1/auth/update-password/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"otp\":\"" + wrong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OTP"));
    }

    private void register(String email, String password) throws Exception {
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, password);
    }

    private String accessToken(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }
}
