package uk.huy.pathwise.core.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T41 acceptance: CORS whitelist, security headers, request id and log context, and {@code X-Forwarded-For} ignored
 * while forward headers are off (the test profile, like dev). {@link ForwardedHeadersIntegrationTest} covers them on.
 */
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = "cors.allowed-origins=https://app.pathwise.test")
class WebHardeningIntegrationTest extends AbstractIntegrationTest {
    private static final String ALLOWED = "https://app.pathwise.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void preflight_whenOriginNotAllowed_reject() throws Exception {
        mockMvc.perform(options("/api/v1/auth/log-in")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void preflight_whenOriginAllowed_allowWithCredentials() throws Exception {
        mockMvc.perform(options("/api/v1/plans/1")
                        .header(HttpHeaders.ORIGIN, ALLOWED)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type, X-Client-Platform"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PATCH")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("X-Client-Platform")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    void request_whenOriginAllowed_exposeRetryAfterAndRequestId() throws Exception {
        mockMvc.perform(post("/api/v1/auth/log-in").header(HttpHeaders.ORIGIN, ALLOWED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@pathwise.test\",\"password\":\"Passw0rd!x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Retry-After")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("X-Request-Id")));
    }

    @Test
    void request_whenOriginNotAllowed_reject() throws Exception {
        mockMvc.perform(post("/api/v1/auth/log-in").header(HttpHeaders.ORIGIN, "https://evil.example")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@pathwise.test\",\"password\":\"Passw0rd!x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void response_always_carrySecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    void response_always_carryNewRequestIdIgnoringClientOne() throws Exception {
        String requestId = mockMvc.perform(get("/api/v1/users/me").header(LogContextFilter.REQUEST_ID_HEADER, "client-chosen"))
                .andExpect(header().string(LogContextFilter.REQUEST_ID_HEADER, not("client-chosen")))
                .andReturn().getResponse().getHeader(LogContextFilter.REQUEST_ID_HEADER);

        assertThat(UUID.fromString(requestId)).isNotNull();
    }

    @Test
    void log_whenAuthenticated_carryRequestUserAndStore(CapturedOutput output) throws Exception {
        TestUsers users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        TestStore store = users.createStore();
        TestUser driver = store.driver();

        // Disabling a member logs "Member disabled" in the OWNER's request.
        String requestId = mockMvc.perform(patch("/api/v1/store/members/" + driver.id())
                        .header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DISABLED\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader(LogContextFilter.REQUEST_ID_HEADER);

        assertThat(output.getOut()).containsPattern("requestId=" + requestId + " userId=" + store.owner().id()
                + " storeId=" + store.id() + "\\].*Member disabled");
    }

    @Test
    void logIn_whenForwardHeadersOff_ignoreXForwardedFor() throws Exception {
        String email = "xff-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, "Passw0rd!x");
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .header("X-Forwarded-For", "203.0.113.7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!x\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(body, "$.data.accessToken");

        mockMvc.perform(get("/api/v1/auth/sessions").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(jsonPath("$.data[0].ipAddress").value("127.0.0.1"));
    }
}
