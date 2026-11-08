package uk.huy.pathwise.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestUser;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code PATCH /users/me} as a merge patch and phone numbers returned as E.164 (API_SPEC §1, §3).
 */
@AutoConfigureMockMvc
class UserProfileIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUser user;

    @BeforeEach
    void setUp() throws Exception {
        user = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender).createStore().driver();
    }

    @Test
    void updateProfile_whenFieldsGiven_saveThemAndReturnProfileWithE164() throws Exception {
        patchMe("{\"fullName\":\"  Tùng \",\"phoneNumber\":\"0905 123 456\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(user.id()))
                .andExpect(jsonPath("$.data.fullName").value("Tùng"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84905123456"));
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer()))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84905123456"));
    }

    @Test
    void updateProfile_whenFieldAbsentOrNull_keepOrClearIt() throws Exception {
        patchMe("{\"fullName\":\"Tùng\",\"phoneNumber\":\"0905123456\"}").andExpect(status().isOk());

        patchMe("{}")
                .andExpect(jsonPath("$.data.fullName").value("Tùng"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84905123456"));
        patchMe("{\"phoneNumber\":null}")
                .andExpect(jsonPath("$.data.fullName").value("Tùng"))
                .andExpect(jsonPath("$.data.phoneNumber").value(nullValue()));
        patchMe("{\"fullName\":null}").andExpect(jsonPath("$.data.fullName").value(nullValue()));
    }

    @Test
    void updateProfile_whenBlankNameOrPut_return400Or405() throws Exception {
        patchMe("{\"fullName\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("fullName"))
                .andExpect(jsonPath("$.details[0].code").value("FULL_NAME_REQUIRED"));
        mockMvc.perform(put("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }

    private ResultActions patchMe(String body) throws Exception {
        return mockMvc.perform(patch("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, user.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
