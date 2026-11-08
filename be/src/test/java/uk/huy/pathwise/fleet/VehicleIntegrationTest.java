package uk.huy.pathwise.fleet;

import com.jayway.jsonpath.JsonPath;
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
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T20 acceptance: vehicles (FR-V01), their tenant isolation and {@link VehicleQuery}.
 */
@AutoConfigureMockMvc
class VehicleIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private VehicleQuery vehicleQuery;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void crud_whenDispatcher_createReadUpdateAndFilterByActive() throws Exception {
        TestStore store = users.createStore();
        TestUser dispatcher = store.dispatcher();

        long id = id(create(dispatcher, "{\"name\":\"  Xe 1 \",\"plateNumber\":\"43A-123.45\",\"capacity\":40,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.capacityUnit").value("BOX"))
                .andExpect(jsonPath("$.data.name").value("Xe 1"))
                .andExpect(jsonPath("$.data.plateNumber").value("43A-123.45"))
                .andExpect(jsonPath("$.data.capacity").value(40))
                .andExpect(jsonPath("$.data.active").value(true)));
        create(dispatcher, "{\"name\":\"Xe 2\",\"capacity\":20,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/vehicles/" + id).header(HttpHeaders.AUTHORIZATION, dispatcher.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));
        update(store.owner(), id, "{\"name\":\"Xe 1\",\"capacity\":35,\"capacityUnit\":\"KG\",\"active\":false}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacityUnit").value("KG"))
                .andExpect(jsonPath("$.data.capacity").value(35))
                .andExpect(jsonPath("$.data.plateNumber").doesNotExist())
                .andExpect(jsonPath("$.data.active").value(false));

        list(dispatcher, "").andExpect(jsonPath("$.data[*].name", contains("Xe 1", "Xe 2")));
        list(dispatcher, "?active=true").andExpect(jsonPath("$.data[*].name", contains("Xe 2")));
        list(dispatcher, "?active=false").andExpect(jsonPath("$.data[*].name", contains("Xe 1")));
    }

    @Test
    void create_whenNameUsedInSameStore_return409EvenInAnotherCase() throws Exception {
        TestStore store = users.createStore();
        create(store.owner(), "{\"name\":\"Xe tải 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());

        create(store.owner(), "{\"name\":\"Xe tải 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_NAME_EXISTED"));
        create(store.dispatcher(), "{\"name\":\"XE TẢI 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_NAME_EXISTED"));
    }

    @Test
    void update_whenRenamedToAnotherVehicleName_return409ButKeepingOwnNameWorks() throws Exception {
        TestStore store = users.createStore();
        create(store.owner(), "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());
        long second = id(create(store.owner(), "{\"name\":\"Xe 2\",\"capacity\":40,\"capacityUnit\":\"BOX\"}"));

        update(store.owner(), second, "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\",\"active\":true}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEHICLE_NAME_EXISTED"));
        update(store.owner(), second, "{\"name\":\"Xe 2\",\"capacity\":50,\"capacityUnit\":\"BOX\",\"active\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(50));
    }

    @Test
    void create_whenSameNameInAnotherStore_succeed() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();

        create(storeA.owner(), "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());
        create(storeB.owner(), "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}").andExpect(status().isCreated());
    }

    @Test
    void vehicle_whenOfAnotherStore_return404AndNeverListed() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();
        long id = id(create(storeA.owner(), "{\"name\":\"Xe A\",\"capacity\":40,\"capacityUnit\":\"BOX\"}"));

        mockMvc.perform(get("/api/v1/vehicles/" + id).header(HttpHeaders.AUTHORIZATION, storeB.dispatcher().bearer()))
                .andExpect(status().isNotFound());
        update(storeB.owner(), id, "{\"name\":\"Stolen\",\"capacity\":1,\"capacityUnit\":\"BOX\",\"active\":false}")
                .andExpect(status().isNotFound());
        list(storeB.owner(), "").andExpect(jsonPath("$.data", hasSize(0)));
        list(storeA.owner(), "").andExpect(jsonPath("$.data[0].name").value("Xe A"));
    }

    @Test
    void vehicles_whenDriverOrAdmin_return403() throws Exception {
        TestStore store = users.createStore();
        TestUser admin = users.createAdmin();
        long id = id(create(store.owner(), "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}"));

        for (TestUser caller : new TestUser[]{store.driver(), admin}) {
            list(caller, "").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
            create(caller, "{\"name\":\"Xe 9\",\"capacity\":40,\"capacityUnit\":\"BOX\"}").andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/vehicles/" + id).header(HttpHeaders.AUTHORIZATION, caller.bearer()))
                    .andExpect(status().isForbidden());
            update(caller, id, "{\"name\":\"Xe 1\",\"capacity\":1,\"capacityUnit\":\"BOX\",\"active\":true}").andExpect(status().isForbidden());
        }
    }

    @Test
    void create_whenCapacityNotPositiveOrNameOrUnitMissing_return400() throws Exception {
        TestStore store = users.createStore();

        create(store.owner(), "{\"name\":\"Xe 1\",\"capacity\":0,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'capacity')].code").value("INVALID_CAPACITY"));
        create(store.owner(), "{\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'name')].code").value("VEHICLE_NAME_REQUIRED"));
        update(store.owner(), 1, "{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'active')].code").value("ACTIVE_REQUIRED"));
        create(store.owner(), "{\"name\":\"Xe 1\",\"capacity\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'capacityUnit')].code").value("CAPACITY_UNIT_REQUIRED"));
        create(store.owner(), "{\"name\":\"Xe 1\",\"capacity\":10,\"capacityUnit\":\"LITER\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void vehicleQuery_whenActiveVehicleOfStore_returnItOtherwiseEmpty() throws Exception {
        TestStore storeA = users.createStore();
        TestStore storeB = users.createStore();
        long active = id(create(storeA.owner(), "{\"name\":\"Xe 1\",\"capacity\":40,\"capacityUnit\":\"BOX\"}"));
        long inactive = id(create(storeA.owner(), "{\"name\":\"Xe 2\",\"capacity\":20,\"capacityUnit\":\"BOX\"}"));
        update(storeA.owner(), inactive, "{\"name\":\"Xe 2\",\"capacity\":20,\"capacityUnit\":\"BOX\",\"active\":false}").andExpect(status().isOk());

        assertThat(vehicleQuery.findActive(storeA.id(), active))
                .contains(new FleetVehicle(active, storeA.id(), "Xe 1", null, 40, CapacityUnit.BOX, true));
        assertThat(vehicleQuery.findActive(storeA.id(), inactive)).isEmpty();
        assertThat(vehicleQuery.findActive(storeB.id(), active)).isEmpty();
        assertThat(vehicleQuery.listActive(storeA.id())).extracting(FleetVehicle::id).containsExactly(active);
    }

    private ResultActions create(TestUser caller, String body) throws Exception {
        return mockMvc.perform(post("/api/v1/vehicles")
                .header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(TestUser caller, long id, String body) throws Exception {
        return mockMvc.perform(put("/api/v1/vehicles/" + id)
                .header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions list(TestUser caller, String query) throws Exception {
        return mockMvc.perform(get("/api/v1/vehicles" + query).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private static long id(ResultActions created) throws Exception {
        String body = created.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }
}
