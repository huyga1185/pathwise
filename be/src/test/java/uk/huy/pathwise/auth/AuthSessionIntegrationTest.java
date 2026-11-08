package uk.huy.pathwise.auth;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T11 acceptance: one session per refresh token family, current marker, revoking one or all other sessions.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
class AuthSessionIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void listSessions_whenLoggedInTwice_returnTwoSessionsWithCurrentMarked() throws Exception {
        String email = register();
        Session first = logIn(email);
        Session second = logIn(email);

        String body = mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, bearer(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        List<String> current = JsonPath.read(body, "$.data[?(@.current == true)].sessionId");
        List<String> others = JsonPath.read(body, "$.data[?(@.current == false)].sessionId");
        assertThat(current).containsExactly(second.sessionId());
        assertThat(others).containsExactly(first.sessionId());
        assertThat((List<String>) JsonPath.read(body, "$.data[*].lastUsedAt")).allSatisfy(t -> assertThat(t).isNotBlank());
    }

    @Test
    void listSessions_whenSessionExpired_excludeIt() throws Exception {
        String email = register();
        Session expired = logIn(email);
        Session active = logIn(email);
        jdbcTemplate.update("UPDATE refresh_tokens SET expires_at = UTC_TIMESTAMP() - INTERVAL 1 DAY WHERE family_id = ?",
                expired.sessionId());

        mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, bearer(active)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sessionId").value(active.sessionId()));
    }

    @Test
    void revokeSession_whenOtherSessionOfSameUser_return204AndItsRefreshTokenStops() throws Exception {
        String email = register();
        Session other = logIn(email);
        Session current = logIn(email);

        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/auth/sessions/{id}", other.sessionId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        refresh(other).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        refresh(current).andExpect(status().isOk());
    }

    @Test
    void revokeSession_whenCurrentSessionFromWeb_actLikeLogOutAndClearCookie() throws Exception {
        String email = register();
        Session other = logIn(email);
        Session current = logIn(email);

        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/auth/sessions/{id}", current.sessionId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).contains("refreshToken=").contains("Max-Age=0");
        assertThat(response.getHeader(HttpHeaders.CACHE_CONTROL)).contains("no-store");
        refresh(current).andExpect(status().isUnauthorized());
        refresh(other).andExpect(status().isOk());
    }

    @Test
    void revokeSession_whenCurrentSessionFromMobile_logOutWithoutCookie() throws Exception {
        Session current = logIn(register());

        MockHttpServletResponse response = mockMvc.perform(delete("/api/v1/auth/sessions/{id}", current.sessionId())
                        .header("X-Client-Platform", "mobile")
                        .header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent())
                .andReturn().getResponse();

        assertThat(response.getHeader(HttpHeaders.SET_COOKIE)).isNull();
        refresh(current).andExpect(status().isUnauthorized());
    }

    @Test
    void revokeSession_whenSessionBelongsToAnotherUser_return404AndKeepIt() throws Exception {
        Session victim = logIn(register());
        Session attacker = logIn(register());

        mockMvc.perform(delete("/api/v1/auth/sessions/{id}", victim.sessionId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(attacker)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        refresh(victim).andExpect(status().isOk());
    }

    @Test
    void revokeSession_whenUnknownSession_return404() throws Exception {
        Session session = logIn(register());

        mockMvc.perform(delete("/api/v1/auth/sessions/{id}", UUID.randomUUID().toString())
                        .header(HttpHeaders.AUTHORIZATION, bearer(session)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void revokeOtherSessions_whenSeveralSessions_keepOnlyCurrent() throws Exception {
        String email = register();
        Session first = logIn(email);
        Session second = logIn(email);
        Session current = logIn(email);

        mockMvc.perform(post("/api/v1/auth/sessions/revoke-others").header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(status().isNoContent());

        refresh(first).andExpect(status().isUnauthorized());
        refresh(second).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, bearer(current)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].current").value(true));
    }

    @Test
    void refresh_whenRotated_accessTokenKeepsSameSession() throws Exception {
        Session session = logIn(register());

        String body = refresh(session).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Session rotated = new Session(JsonPath.read(body, "$.data.accessToken"), JsonPath.read(body, "$.data.refreshToken"),
                session.sessionId());

        mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, bearer(rotated)))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].sessionId").value(session.sessionId()))
                .andExpect(jsonPath("$.data[0].current").value(true));
    }

    @Test
    void sessions_whenNoAccessToken_return401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/sessions")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/sessions/revoke-others")).andExpect(status().isUnauthorized());
    }

    private String register() throws Exception {
        String email = "t11-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        return email;
    }

    /**
     * Mobile log-in so the refresh token is in the body; the session id is the family of that token.
     */
    private Session logIn(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = JsonPath.read(body, "$.data.refreshToken");
        long userId = ((Number) JsonPath.read(body, "$.data.user.id")).longValue();
        String sessionId = jdbcTemplate.queryForObject(
                "SELECT family_id FROM refresh_tokens WHERE user_id = ? ORDER BY id DESC LIMIT 1", String.class, userId);
        return new Session(JsonPath.read(body, "$.data.accessToken"), refreshToken, sessionId);
    }

    private ResultActions refresh(Session session) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                .header("X-Client-Platform", "mobile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + session.refreshToken() + "\"}"));
    }

    private static String bearer(Session session) {
        return "Bearer " + session.accessToken();
    }

    private record Session(String accessToken, String refreshToken, String sessionId) {}
}
