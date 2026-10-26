package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T10 acceptance: refresh token transport (web cookie / mobile body), expiry, reuse detection and log-out.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
class AuthTokenFlowIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";
    private static final String MOBILE = "mobile";
    private static final String PLATFORM_HEADER = "X-Client-Platform";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void logIn_whenWebClient_setRefreshCookieOnAuthPathAndOmitTokenFromBody() throws Exception {
        String email = register();

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/log-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.accessTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshTokenExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("OWNER"))
                .andReturn().getResponse();

        String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .startsWith("refreshToken=")
                .contains("Path=/api/v1/auth;")
                .contains("SameSite=Strict")
                .contains("HttpOnly")
                .contains("Secure");
    }

    @Test
    void logIn_whenMobileClient_returnRefreshTokenInBodyWithoutCookie() throws Exception {
        String email = register();

        MockHttpServletResponse response = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void refresh_whenMobileClient_rotateTokenThroughBody() throws Exception {
        LoggedIn session = logInMobile(register());

        String rotated = JsonPath.read(mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(session.refreshToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user").doesNotExist())
                .andReturn().getResponse().getContentAsString(), "$.data.refreshToken");

        assertThat(rotated).isNotBlank().isNotEqualTo(session.refreshToken());
        assertThat(states(session.userId())).containsExactlyInAnyOrder("USED", "ACTIVE");
    }

    @Test
    void refresh_whenWebCookie_rotateTokenAndSetNewCookie() throws Exception {
        MockHttpServletResponse login = mockMvc.perform(post("/api/v1/auth/log-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(register())))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Cookie original = login.getCookie("refreshToken");
        assertThat(original).isNotNull();

        MockHttpServletResponse refresh = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", original.getValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andReturn().getResponse();

        Cookie rotated = refresh.getCookie("refreshToken");
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());
        assertThat(refresh.getHeader(HttpHeaders.SET_COOKIE)).contains("Path=/api/v1/auth;").contains("SameSite=Strict");
    }

    @Test
    void refresh_whenTokenExpired_return401AndMarkTokenExpired() throws Exception {
        LoggedIn session = logInMobile(register());
        jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = UTC_TIMESTAMP() - INTERVAL 1 DAY WHERE user_id = ?",
                session.userId());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(session.refreshToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));

        assertThat(states(session.userId())).containsExactly("EXPIRED");
    }

    @Test
    void refresh_whenUsedTokenReusedAfterGrace_return401AndRevokeFamily() throws Exception {
        LoggedIn session = logInMobile(register());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(session.refreshToken())))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE refresh_tokens SET rotated_at = UTC_TIMESTAMP() - INTERVAL 1 MINUTE "
                + "WHERE user_id = ? AND state = 'USED'", session.userId());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(session.refreshToken())))
                .andExpect(status().isUnauthorized());

        assertThat(states(session.userId())).containsOnly("REVOKED").hasSize(2);
    }

    @Test
    void logOut_whenWebCookieWithoutAccessToken_return204ClearCookieAndRevokeFamily() throws Exception {
        String email = register();
        MockHttpServletResponse login = mockMvc.perform(post("/api/v1/auth/log-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        long userId = ((Number) JsonPath.read(login.getContentAsString(), "$.data.user.id")).longValue();
        Cookie cookie = login.getCookie("refreshToken");
        assertThat(cookie).isNotNull();

        MockHttpServletResponse logout = mockMvc.perform(post("/api/v1/auth/log-out")
                        .cookie(new Cookie("refreshToken", cookie.getValue())))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(logout.getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0").contains("Path=/api/v1/auth;");
        assertThat(states(userId)).containsExactly("REVOKED");
    }

    @Test
    void logOut_whenMobileBody_return204AndRevokeFamily() throws Exception {
        LoggedIn session = logInMobile(register());

        MockHttpServletResponse logout = mockMvc.perform(post("/api/v1/auth/log-out")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody(session.refreshToken())))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(logout.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(states(session.userId())).containsExactly("REVOKED");
    }

    @Test
    void logOut_whenNoRefreshToken_return204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/log-out"))
                .andExpect(status().isNoContent());
    }

    private String register() throws Exception {
        String email = "t10-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        return email;
    }

    private LoggedIn logInMobile(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header(PLATFORM_HEADER, MOBILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new LoggedIn(((Number) JsonPath.read(body, "$.data.user.id")).longValue(),
                JsonPath.read(body, "$.data.refreshToken"));
    }

    private List<String> states(long userId) {
        return jdbcTemplate.queryForList("SELECT state FROM refresh_tokens WHERE user_id = ?", String.class, userId);
    }

    private static String credentials(String email) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
    }

    private static String refreshBody(String refreshToken) {
        return "{\"refreshToken\":\"" + refreshToken + "\"}";
    }

    private record LoggedIn(long userId, String refreshToken) {}
}
