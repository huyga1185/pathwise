package uk.huy.pathwise.store;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.service.StoreMemberQuery;
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
 * {@code GET/PUT /store} (FR-S01) and {@link StoreMemberQuery}. The store always comes from the access token.
 */
@AutoConfigureMockMvc
class StoreIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Passw0rd!x";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StoreMemberQuery storeMemberQuery;

    @Test
    void getStore_whenOwner_returnOwnStoreWithDefaults() throws Exception {
        Owner owner = newOwner();

        getStore(owner.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(owner.storeId()))
                .andExpect(jsonPath("$.data.name").value("Cửa hàng " + owner.email()))
                .andExpect(jsonPath("$.data.depotLat").doesNotExist())
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"));
    }

    @Test
    void updateStore_whenValid_saveAndReturnStore() throws Exception {
        Owner owner = newOwner();

        updateStore(owner.accessToken(), "{\"name\":\"Tiệm Mới\",\"depotAddress\":\"12 Lê Duẩn, Đà Nẵng\","
                + "\"depotLat\":16.05,\"depotLon\":108.2,\"timezone\":\"Asia/Bangkok\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Tiệm Mới"))
                .andExpect(jsonPath("$.data.depotLat").value(16.05))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Bangkok"));

        getStore(owner.accessToken())
                .andExpect(jsonPath("$.data.depotAddress").value("12 Lê Duẩn, Đà Nẵng"))
                .andExpect(jsonPath("$.data.depotLon").value(108.2));
    }

    @Test
    void updateStore_whenOtherStoreUpdated_ownStoreUnchanged() throws Exception {
        Owner ownerA = newOwner();
        Owner ownerB = newOwner();

        updateStore(ownerA.accessToken(), "{\"name\":\"Store A\",\"timezone\":\"Asia/Ho_Chi_Minh\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownerA.storeId()));

        getStore(ownerB.accessToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(ownerB.storeId()))
                .andExpect(jsonPath("$.data.name").value("Cửa hàng " + ownerB.email()));
    }

    @Test
    void updateStore_whenUnknownTimezone_return400InvalidTimezone() throws Exception {
        Owner owner = newOwner();

        updateStore(owner.accessToken(), "{\"name\":\"Shop\",\"timezone\":\"Mars/Base\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
    }

    @Test
    void updateStore_whenOnlyLatitude_return400DepotCoordinatesIncomplete() throws Exception {
        Owner owner = newOwner();

        updateStore(owner.accessToken(), "{\"name\":\"Shop\",\"depotLat\":16.05,\"timezone\":\"Asia/Ho_Chi_Minh\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].code").value("DEPOT_COORDINATES_INCOMPLETE"));
    }

    @Test
    void getStore_whenAdminWithoutStore_return403() throws Exception {
        String email = "t13-admin-" + UUID.randomUUID() + "@pathwise.test";
        jdbcTemplate.update("INSERT INTO users (email, password, role, status, email_verified_at, created_at, updated_at) "
                        + "VALUES (?, ?, 'ADMIN', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                email, passwordEncoder.encode(PASSWORD));

        String token = accessToken(email);
        getStore(token)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void getStore_whenNoAccessToken_return401() throws Exception {
        mockMvc.perform(get("/api/v1/store")).andExpect(status().isUnauthorized());
    }

    @Test
    void findActiveMember_whenActiveOwnerOfStore_returnMember() throws Exception {
        Owner owner = newOwner();

        assertThat(storeMemberQuery.findActiveMember(owner.storeId(), owner.userId()))
                .contains(new StoreMember(owner.userId(), owner.email(), null, UserRole.OWNER, UserStatus.ACTIVE, false));
    }

    @Test
    void findActiveMember_whenUserOfAnotherStore_returnEmpty() throws Exception {
        Owner ownerA = newOwner();
        Owner ownerB = newOwner();

        assertThat(storeMemberQuery.findActiveMember(ownerA.storeId(), ownerB.userId())).isEmpty();
    }

    @Test
    void findActiveMember_whenNotActive_returnEmpty() throws Exception {
        String email = "t13-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.register(mockMvc, email, PASSWORD, "Shop");
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        long storeId = jdbcTemplate.queryForObject("SELECT store_id FROM users WHERE email = ?", Long.class, email);

        assertThat(storeMemberQuery.findActiveMember(storeId, userId)).isEmpty();
    }

    private Owner newOwner() throws Exception {
        String email = "t13-" + UUID.randomUUID() + "@pathwise.test";
        TestAccounts.registerVerifiedOwner(mockMvc, emailSender, email, PASSWORD);
        long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        long storeId = jdbcTemplate.queryForObject("SELECT store_id FROM users WHERE email = ?", Long.class, email);
        return new Owner(email, userId, storeId, accessToken(email));
    }

    private String accessToken(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in")
                        .header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.accessToken");
    }

    private ResultActions getStore(String accessToken) throws Exception {
        return mockMvc.perform(get("/api/v1/store").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));
    }

    private ResultActions updateStore(String accessToken, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/store")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private record Owner(String email, long userId, long storeId, String accessToken) {}
}
