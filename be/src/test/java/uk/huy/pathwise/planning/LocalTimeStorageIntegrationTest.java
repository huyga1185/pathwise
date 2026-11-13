package uk.huy.pathwise.planning;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-54 (T50): times of day (shifts, time windows) are stored as written, whatever the JVM's time zone. The JVM runs
 * in Asia/Ho_Chi_Minh here, as on a developer's machine: in 1970, the date {@code java.sql.Time} uses, Vietnam was
 * UTC+8, so a conversion through it moved 08:00 to 00:00.
 */
@AutoConfigureMockMvc
class LocalTimeStorageIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TimeZone previous;

    @BeforeEach
    void setUp() {
        previous = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    @AfterEach
    void tearDown() {
        TimeZone.setDefault(previous);
    }

    @Test
    void savePlan_whenJvmNotUtc_storeTimesOfDayAsWrittenAndReadThemBack() throws Exception {
        TestUsers users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        String vehicle = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}"))
                .andReturn().getResponse().getContentAsString();
        long vehicleId = ((Number) JsonPath.read(vehicle, "$.data.id")).longValue();
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Giao sáng\",\"planDate\":\"2026-11-02\","
                                + "\"stops\":[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21,"
                                + "\"timeWindow\":{\"start\":\"09:30\",\"end\":\"11:00\"}}],"
                                + "\"assignments\":[{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                                .formatted(store.driver().id(), vehicleId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        // Read as text by MariaDB itself: no driver conversion involved.
        Map<String, Object> route = jdbcTemplate.queryForMap(
                "SELECT TIME_FORMAT(shift_start, '%H:%i') AS s, TIME_FORMAT(shift_end, '%H:%i') AS e FROM route_plan_routes WHERE plan_id = ?",
                planId);
        Map<String, Object> stop = jdbcTemplate.queryForMap(
                "SELECT TIME_FORMAT(tw_start, '%H:%i') AS s, TIME_FORMAT(tw_end, '%H:%i') AS e FROM route_plan_stops WHERE plan_id = ?",
                planId);
        assertThat(route).containsEntry("s", "08:00").containsEntry("e", "17:00");
        assertThat(stop).containsEntry("s", "09:30").containsEntry("e", "11:00");
        // Dates are not shifted either.
        assertThat(jdbcTemplate.queryForObject("SELECT DATE_FORMAT(plan_date, '%Y-%m-%d') FROM route_plans WHERE id = ?",
                String.class, planId)).isEqualTo("2026-11-02");

        mockMvc.perform(get("/api/v1/plans/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.routes[0].shiftStart").value("08:00"))
                .andExpect(jsonPath("$.data.routes[0].shiftEnd").value("17:00"))
                .andExpect(jsonPath("$.data.unassignedStops[0].timeWindow.start").value("09:30"));
    }

    @Test
    void readPlan_whenTimeWrittenBySql_returnSameTimeOfDay() throws Exception {
        TestUsers users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depot\":{\"address\":\"Kho\",\"lat\":16.05,\"lon\":108.2},"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        String vehicle = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}"))
                .andReturn().getResponse().getContentAsString();
        long vehicleId = ((Number) JsonPath.read(vehicle, "$.data.id")).longValue();
        String body = mockMvc.perform(post("/api/v1/plans").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"name\":\"Giao sáng\",\"planDate\":\"2026-11-02\",\"stops\":[],"
                                + "\"assignments\":[{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\"}]}")
                                .formatted(store.driver().id(), vehicleId)))
                .andReturn().getResponse().getContentAsString();
        long planId = ((Number) JsonPath.read(body, "$.data.id")).longValue();

        jdbcTemplate.update("UPDATE route_plan_routes SET shift_end = '16:45' WHERE plan_id = ?", planId);

        mockMvc.perform(get("/api/v1/plans/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.routes[0].shiftEnd").value("16:45"));
    }
}
