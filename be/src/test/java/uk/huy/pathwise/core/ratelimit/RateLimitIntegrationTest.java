package uk.huy.pathwise.core.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T40 acceptance: rate limits of the auth endpoints and invitations (P-16, API_SPEC §2, §4). Requests under test come
 * from their own remote address; the helpers that create accounts use the default IP, whose limits
 * stay above what they need.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "rate-limit.rules.log-in-ip.limit=10",
        "rate-limit.rules.log-in-email.limit=2",
        "rate-limit.rules.register-ip.limit=2",
        "rate-limit.rules.resend-verification-ip.limit=3",
        "rate-limit.rules.resend-verification-email.limit=2",
        "rate-limit.rules.forgot-password-ip.limit=3",
        "rate-limit.rules.forgot-password-email.limit=2",
        "rate-limit.rules.change-password-otp-user.limit=2",
        "rate-limit.rules.create-invitation-store.limit=2",
        "rate-limit.rules.directions-user.limit=2"})
class RateLimitIntegrationTest extends AbstractIntegrationTest {
    private static final String AUTH = "/api/v1/auth";

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

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        Set<String> keys = redisTemplate.keys("ratelimit:*");
        if (!keys.isEmpty()) redisTemplate.delete(keys);
    }

    @Test
    void logIn_whenEmailLimitExceeded_return429WithRetryAfterWhateverTheCase() throws Exception {
        String email = newEmail();

        logIn(newIp(), email).andExpect(status().isUnauthorized());
        logIn(newIp(), email.toUpperCase()).andExpect(status().isUnauthorized());
        // 15 min window, rounded up to whole seconds.
        logIn(newIp(), email)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, matchesPattern("8\\d\\d|900")));
    }

    @Test
    void logIn_whenIpLimitExceeded_return429ForAnyEmailFromThatIpOnly() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 10; i++) logIn(ip, newEmail()).andExpect(status().isUnauthorized());

        logIn(ip, newEmail()).andExpect(status().isTooManyRequests());
        logIn(newIp(), newEmail()).andExpect(status().isUnauthorized());
    }

    @Test
    void register_whenIpLimitExceeded_return429AndCreateNothing() throws Exception {
        String ip = newIp();
        register(ip, newEmail()).andExpect(status().isCreated());
        register(ip, newEmail()).andExpect(status().isCreated());
        String blocked = newEmail();

        register(ip, blocked).andExpect(status().isTooManyRequests());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, blocked)).isZero();
    }

    @Test
    void forgotPassword_whenEmailLimitExceeded_return429EvenForUnknownEmail() throws Exception {
        String email = newEmail();
        forgotPassword(newIp(), email).andExpect(status().isNoContent());
        forgotPassword(newIp(), email).andExpect(status().isNoContent());

        forgotPassword(newIp(), email).andExpect(status().isTooManyRequests());
    }

    @Test
    void forgotPassword_whenIpLimitExceeded_return429() throws Exception {
        String ip = newIp();
        for (int i = 0; i < 3; i++) forgotPassword(ip, newEmail()).andExpect(status().isNoContent());

        forgotPassword(ip, newEmail()).andExpect(status().isTooManyRequests());
    }

    @Test
    void resendVerification_whenEmailOrIpLimitExceeded_return429() throws Exception {
        String email = newEmail();
        resendVerification(newIp(), email).andExpect(status().isNoContent());
        resendVerification(newIp(), email).andExpect(status().isNoContent());
        resendVerification(newIp(), email).andExpect(status().isTooManyRequests());

        String ip = newIp();
        for (int i = 0; i < 3; i++) resendVerification(ip, newEmail()).andExpect(status().isNoContent());
        resendVerification(ip, newEmail()).andExpect(status().isTooManyRequests());
    }

    @Test
    void requestPasswordChangeOtp_whenUserLimitExceeded_return429BeforeCheckingPassword() throws Exception {
        TestStore store = users.createStore();
        for (int i = 0; i < 2; i++) {
            changePasswordOtp(store.owner().bearer(), "wrong-password")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));
        }

        changePasswordOtp(store.owner().bearer(), "wrong-password")
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        changePasswordOtp(store.dispatcher().bearer(), "wrong-password").andExpect(status().isBadRequest());
    }

    @Test
    void createInvitation_whenStoreLimitExceeded_return429ForThatStoreOnly() throws Exception {
        TestStore store = users.createStore();
        invite(store.owner().bearer(), newEmail()).andExpect(status().isCreated());
        invite(store.owner().bearer(), newEmail()).andExpect(status().isCreated());

        invite(store.owner().bearer(), newEmail()).andExpect(status().isTooManyRequests());
        TestStore other = users.createStore();
        invite(other.owner().bearer(), newEmail()).andExpect(status().isCreated());
    }

    @Test
    void directions_whenDriverLimitExceeded_return429ForThatDriverOnly() throws Exception {
        // The limit is checked before the route is looked up: an unknown route still counts.
        TestStore store = users.createStore();
        directions(store.driver().bearer()).andExpect(status().isNotFound());
        directions(store.driver().bearer()).andExpect(status().isNotFound());

        directions(store.driver().bearer()).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        TestStore other = users.createStore();
        directions(other.driver().bearer()).andExpect(status().isNotFound());
    }

    @Test
    void check_whenCounting_keyHasTtlAndNoEmailInIt() throws Exception {
        String email = newEmail();
        forgotPassword(newIp(), email).andExpect(status().isNoContent());

        Set<String> keys = redisTemplate.keys("ratelimit:forgot-password-email:*");
        assertThat(keys).hasSize(1);
        String key = keys.iterator().next();
        assertThat(key).doesNotContain(email);
        assertThat(redisTemplate.getExpire(key, TimeUnit.SECONDS)).isBetween(1L, 3600L);
    }

    private ResultActions logIn(String ip, String email) throws Exception {
        return postFrom(ip, AUTH + "/log-in", "{\"email\":\"" + email + "\",\"password\":\"Passw0rd!x\"}");
    }

    private ResultActions register(String ip, String email) throws Exception {
        return postFrom(ip, AUTH + "/register", "{\"email\":\"" + email + "\",\"password\":\"password123\",\"storeName\":\"Shop\"}");
    }

    private ResultActions forgotPassword(String ip, String email) throws Exception {
        return postFrom(ip, AUTH + "/password/forgot", "{\"email\":\"" + email + "\"}");
    }

    private ResultActions resendVerification(String ip, String email) throws Exception {
        return postFrom(ip, AUTH + "/email/verify/resend", "{\"email\":\"" + email + "\"}");
    }

    private ResultActions changePasswordOtp(String bearer, String currentPassword) throws Exception {
        return mockMvc.perform(post(AUTH + "/password/change/otp").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"" + currentPassword + "\"}"));
    }

    private ResultActions directions(String bearer) throws Exception {
        return mockMvc.perform(get("/api/v1/driver/routes/999999/directions?lat=16.05&lon=108.2")
                .header(HttpHeaders.AUTHORIZATION, bearer));
    }

    private ResultActions invite(String bearer, String email) throws Exception {
        return mockMvc.perform(post("/api/v1/store/invitations").header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"role\":\"DRIVER\"}"));
    }

    private ResultActions postFrom(String ip, String path, String body) throws Exception {
        return mockMvc.perform(post(path)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String newEmail() {
        return "rl-" + UUID.randomUUID() + "@pathwise.test";
    }

    private static String newIp() {
        UUID random = UUID.randomUUID();
        return "10." + (random.getMostSignificantBits() & 0xff) + "." + (random.getLeastSignificantBits() & 0xff) + "."
                + ((random.getLeastSignificantBits() >>> 8) & 0xff);
    }
}
