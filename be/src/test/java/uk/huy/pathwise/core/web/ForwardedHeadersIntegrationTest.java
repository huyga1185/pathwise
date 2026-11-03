package uk.huy.pathwise.core.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.support.AbstractIntegrationTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T41: with {@code server.forward-headers-strategy=native} (staging, prod), Tomcat takes the client IP from
 * {@code X-Forwarded-For} sent by a trusted proxy, here the test client on 127.0.0.1. Needs a real server: MockMvc
 * does not run Tomcat's valve.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "server.forward-headers-strategy=native")
class ForwardedHeadersIntegrationTest extends AbstractIntegrationTest {
    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void logIn_whenForwardedByTrustedProxy_recordClientIp() {
        RestClient client = RestClient.create("http://127.0.0.1:" + port);
        String email = "proxy-" + UUID.randomUUID() + "@pathwise.test";
        // An ACTIVE user without a store, as TestUsers does: the request under test is the log-in.
        jdbcTemplate.update("INSERT INTO users (email, password, role, status, email_verified_at, can_drive, created_at, updated_at) "
                        + "VALUES (?, ?, 'ADMIN', 'ACTIVE', UTC_TIMESTAMP(6), false, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                email, passwordEncoder.encode("Passw0rd!x"));

        String body = client.post().uri("/api/v1/auth/log-in")
                .header("X-Client-Platform", "mobile")
                .header("X-Forwarded-For", "203.0.113.7")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"Passw0rd!x\"}")
                .retrieve().body(String.class);
        String sessions = client.get().uri("/api/v1/auth/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + JsonPath.read(body, "$.data.accessToken"))
                .retrieve().body(String.class);

        assertThat((String) JsonPath.read(sessions, "$.data[0].ipAddress")).isEqualTo("203.0.113.7");
    }
}
