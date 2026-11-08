package uk.huy.pathwise.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.shared.identity.UserRole;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Builds stores and users of every role, each with an access token from a real log-in, for authorization and tenant
 * isolation tests (T17). Usage in an integration test with {@code @AutoConfigureMockMvc}:
 * <pre>{@code
 * TestUsers users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
 * TestStore storeA = users.createStore();
 * TestStore storeB = users.createStore();
 * mockMvc.perform(get("/api/v1/...").header(AUTHORIZATION, storeA.dispatcher().bearer()));
 * }</pre>
 * The OWNER registers through the API. DISPATCHER and DRIVER are inserted directly until invitations exist (T16).
 */
public class TestUsers {
    public static final String PASSWORD = "Passw0rd!x";

    private final MockMvc mockMvc;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final FakeEmailSender emailSender;

    public TestUsers(MockMvc mockMvc, JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, FakeEmailSender emailSender) {
        this.mockMvc = mockMvc;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
    }

    public record TestUser(long id, String email, UserRole role, Long storeId, String accessToken) {
        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    public record TestStore(long id, TestUser owner, TestUser dispatcher, TestUser driver) {}

    /**
     * A new store with an ACTIVE OWNER, DISPATCHER and DRIVER, none of them with {@code canDrive}.
     */
    public TestStore createStore() throws Exception {
        String ownerEmail = newEmail("owner");
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, ownerEmail, PASSWORD);
        long storeId = jdbcTemplate.queryForObject("SELECT store_id FROM users WHERE email = ?", Long.class, ownerEmail);
        TestUser owner = logIn(ownerEmail, UserRole.OWNER, storeId);
        return new TestStore(storeId,
                owner,
                addMember(storeId, UserRole.DISPATCHER, false),
                addMember(storeId, UserRole.DRIVER, false));
    }

    /**
     * An ACTIVE member of {@code storeId}, logged in.
     */
    public TestUser addMember(long storeId, UserRole role, boolean canDrive) throws Exception {
        String email = newEmail(role.name().toLowerCase());
        insertUser(email, role, storeId, canDrive);
        return logIn(email, role, storeId);
    }

    /**
     * A platform ADMIN (no store), logged in.
     */
    public TestUser createAdmin() throws Exception {
        String email = newEmail("admin");
        insertUser(email, UserRole.ADMIN, null, false);
        return logIn(email, UserRole.ADMIN, null);
    }

    /**
     * Sets {@code can_drive} and logs in again, because the flag travels in the access token.
     */
    public TestUser withCanDrive(TestUser user, boolean canDrive) throws Exception {
        jdbcTemplate.update("UPDATE users SET can_drive = ? WHERE id = ?", canDrive, user.id());
        return logIn(user.email(), user.role(), user.storeId());
    }

    private void insertUser(String email, UserRole role, Long storeId, boolean canDrive) {
        jdbcTemplate.update("INSERT INTO users (email, password, role, status, store_id, email_verified_at, can_drive, "
                        + "created_at, updated_at) VALUES (?, ?, ?, 'ACTIVE', ?, UTC_TIMESTAMP(6), ?, UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                email, passwordEncoder.encode(PASSWORD), role.name(), storeId, canDrive);
    }

    private TestUser logIn(String email, UserRole role, Long storeId) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.data.user.id")).longValue();
        return new TestUser(id, email, role, storeId, JsonPath.read(body, "$.data.accessToken"));
    }

    private static String newEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@pathwise.test";
    }
}
