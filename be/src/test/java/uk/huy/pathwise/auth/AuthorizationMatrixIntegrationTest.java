package uk.huy.pathwise.auth;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.Map;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T17: the permission matrix of ARCHITECTURE.md §6.3 for the endpoints that exist, one case per cell, with real
 * access tokens. The legacy {@code /route/optimize} endpoints are covered by {@code OptimizationControllerTest}
 * (they would call Nominatim here). Also the reference tenant isolation test for store-scoped endpoints.
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationMatrixIntegrationTest extends AbstractIntegrationTest {
    private static final String STORE_BODY = "{\"name\":\"Matrix\",\"timezone\":\"Asia/Ho_Chi_Minh\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;
    private Map<String, TestUser> actors;

    @BeforeAll
    void createActors() throws Exception {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        TestStore store = users.createStore();
        actors = Map.of("ADMIN", users.createAdmin(),
                "OWNER", store.owner(),
                "DISPATCHER", store.dispatcher(),
                "DRIVER", store.driver());
    }

    static Stream<Arguments> matrix() {
        return Stream.of(
                // Own profile and sessions: every role.
                Arguments.of("GET", "/api/v1/users/me", "ADMIN", 200),
                Arguments.of("GET", "/api/v1/users/me", "OWNER", 200),
                Arguments.of("GET", "/api/v1/users/me", "DISPATCHER", 200),
                Arguments.of("GET", "/api/v1/users/me", "DRIVER", 200),
                Arguments.of("GET", "/api/v1/auth/sessions", "ADMIN", 200),
                Arguments.of("GET", "/api/v1/auth/sessions", "OWNER", 200),
                Arguments.of("GET", "/api/v1/auth/sessions", "DISPATCHER", 200),
                Arguments.of("GET", "/api/v1/auth/sessions", "DRIVER", 200),
                // Store: OWNER reads and edits, DISPATCHER reads, ADMIN and DRIVER nothing.
                Arguments.of("GET", "/api/v1/store", "ADMIN", 403),
                Arguments.of("GET", "/api/v1/store", "OWNER", 200),
                Arguments.of("GET", "/api/v1/store", "DISPATCHER", 200),
                Arguments.of("GET", "/api/v1/store", "DRIVER", 403),
                Arguments.of("PUT", "/api/v1/store", "ADMIN", 403),
                Arguments.of("PUT", "/api/v1/store", "OWNER", 200),
                Arguments.of("PUT", "/api/v1/store", "DISPATCHER", 403),
                Arguments.of("PUT", "/api/v1/store", "DRIVER", 403),
                // Users of the whole platform: ADMIN only.
                Arguments.of("GET", "/api/v1/admin/users", "ADMIN", 200),
                Arguments.of("GET", "/api/v1/admin/users", "OWNER", 403),
                Arguments.of("GET", "/api/v1/admin/users", "DISPATCHER", 403),
                Arguments.of("GET", "/api/v1/admin/users", "DRIVER", 403));
    }

    @ParameterizedTest(name = "{2} {0} {1} -> {3}")
    @MethodSource("matrix")
    void endpoint_whenCalledByRole_followPermissionMatrix(String method, String path, String role, int expected) throws Exception {
        var request = request(HttpMethod.valueOf(method), path)
                .header(HttpHeaders.AUTHORIZATION, actors.get(role).bearer());
        if (method.equals("PUT")) request.contentType(MediaType.APPLICATION_JSON).content(STORE_BODY);

        var result = mockMvc.perform(request).andExpect(status().is(expected));
        if (expected == 403) result.andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    /**
     * Reference tenant isolation test: the store always comes from the token, so members of store B never see or
     * change store A, whatever store A's members do.
     */
    @Test
    void store_whenTwoStores_eachMemberSeesOnlyOwnStore() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();

        mockMvc.perform(put("/api/v1/store")
                        .header(HttpHeaders.AUTHORIZATION, storeA.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Store A\",\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storeA.id()));

        mockMvc.perform(get("/api/v1/store").header(HttpHeaders.AUTHORIZATION, storeA.dispatcher().bearer()))
                .andExpect(jsonPath("$.data.id").value(storeA.id()))
                .andExpect(jsonPath("$.data.name").value("Store A"));
        mockMvc.perform(get("/api/v1/store").header(HttpHeaders.AUTHORIZATION, storeB.owner().bearer()))
                .andExpect(jsonPath("$.data.id").value(storeB.id()))
                .andExpect(jsonPath("$.data.name").value("Cửa hàng " + storeB.owner().email()));
        mockMvc.perform(get("/api/v1/store").header(HttpHeaders.AUTHORIZATION, storeB.dispatcher().bearer()))
                .andExpect(jsonPath("$.data.id").value(storeB.id()));
    }

    @Test
    void logIn_whenOwnerHasCanDrive_tokenAndResponseCarryIt() throws Exception {
        TestStore store = users.createStore();
        TestUser driving = users.withCanDrive(store.owner(), true);

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, driving.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canDrive").value(true));
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, store.driver().bearer()))
                .andExpect(jsonPath("$.data.canDrive").value(true));
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, store.dispatcher().bearer()))
                .andExpect(jsonPath("$.data.canDrive").value(false));
    }
}
