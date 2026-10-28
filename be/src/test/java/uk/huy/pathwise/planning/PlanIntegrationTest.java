package uk.huy.pathwise.planning;

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
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T24 acceptance: plan drafts (FR-P02, FR-P03, FR-P07), their business rules (BR-07..BR-09, BR-13) and tenant
 * isolation.
 */
@AutoConfigureMockMvc
class PlanIntegrationTest extends AbstractIntegrationTest {
    private static final String PLANS = "/api/v1/plans";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private TestUsers users;

    @BeforeEach
    void setUp() {
        users = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender);
    }

    @Test
    void create_whenValidDraft_returnDraftWithSnapshotsAndUnassignedStops() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        String stops = """
                [{"label":"ĐH-1","recipientName":"Chị Lan","recipientPhone":"0905 123 456","address":" 12 Lê Duẩn, Đà Nẵng ",
                  "demands":{"BOX":3,"KG":0},"serviceTimeSeconds":300,"timeWindow":{"start":"09:00","end":"11:00"},"note":"Gọi trước"},
                 {"label":"ĐH-2","lat":16.06,"lon":108.22}]""";

        String body = create(store.dispatcher(), draft("Giao sáng", stops, assignment(store.driver().id(), vehicle)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.processingStatus").value("IDLE"))
                .andExpect(jsonPath("$.data.planDate").value("2026-11-02"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.data.depot.address").value("Kho Hải Châu"))
                .andExpect(jsonPath("$.data.depot.lat").value(16.05))
                .andExpect(jsonPath("$.data.optimizedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.geocodingSummary.total").value(2))
                .andExpect(jsonPath("$.data.geocodingSummary.pending").value(1))
                .andExpect(jsonPath("$.data.geocodingSummary.provided").value(1))
                .andExpect(jsonPath("$.data.routes", hasSize(1)))
                .andExpect(jsonPath("$.data.routes[0].driver.id").value(store.driver().id()))
                .andExpect(jsonPath("$.data.routes[0].driver.role").value("DRIVER"))
                .andExpect(jsonPath("$.data.routes[0].vehicle.name").value("Xe 1"))
                .andExpect(jsonPath("$.data.routes[0].vehicle.capacity").value(40))
                .andExpect(jsonPath("$.data.routes[0].vehicle.capacityUnit").value("BOX"))
                .andExpect(jsonPath("$.data.routes[0].shiftStart").value("08:00"))
                .andExpect(jsonPath("$.data.routes[0].start.lon").value(108.2))
                .andExpect(jsonPath("$.data.routes[0].end.address").value("Kho Hải Châu"))
                .andExpect(jsonPath("$.data.routes[0].load").value(nullValue()))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(0)))
                .andExpect(jsonPath("$.data.unassignedStops[*].label", contains("ĐH-1", "ĐH-2")))
                .andExpect(jsonPath("$.data.unassignedStops[0].address").value("12 Lê Duẩn, Đà Nẵng"))
                .andExpect(jsonPath("$.data.unassignedStops[0].recipientPhone").value("+84905123456"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.unassignedStops[0].demands.BOX").value(3))
                .andExpect(jsonPath("$.data.unassignedStops[0].demands.KG").doesNotExist())
                .andExpect(jsonPath("$.data.unassignedStops[0].timeWindow.end").value("11:00"))
                .andExpect(jsonPath("$.data.unassignedStops[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("NOT_OPTIMIZED"))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodeStatus").value("PROVIDED"))
                .andExpect(jsonPath("$.data.unassignedStops[1].serviceTimeSeconds").value(0))
                .andExpect(jsonPath("$.data.unassignedStops[1].timeWindow").value(nullValue()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(PLANS + "/" + id(body)).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(2)))
                .andExpect(jsonPath("$.data.routes[0].vehicle.id").value(vehicle));
    }

    @Test
    void create_whenDepotGiven_useItEvenIfStoreHasNone() throws Exception {
        TestStore store = users.createStore();

        create(store.owner(), draft("Không depot", "[]", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("STORE_DEPOT_REQUIRED"));
        create(store.owner(), """
                {"name":"Có depot","planDate":"2026-11-02","depot":{"address":"Kho tạm","lat":10.77,"lon":106.7}}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.depot.address").value("Kho tạm"))
                .andExpect(jsonPath("$.data.depot.lat").value(10.77))
                .andExpect(jsonPath("$.data.routes", hasSize(0)))
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(0)));
    }

    @Test
    void create_whenDriverOrVehicleOfAnotherStore_return422InvalidAssignment() throws Exception {
        TestStore storeA = storeWithDepot();
        TestStore storeB = storeWithDepot();
        long vehicleA = createVehicle(storeA.owner(), "Xe A", 40, "BOX");
        long vehicleB = createVehicle(storeB.owner(), "Xe B", 40, "BOX");

        create(storeA.owner(), draft("P", "[]", assignment(storeB.driver().id(), vehicleA)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
        create(storeA.owner(), draft("P", "[]", assignment(storeA.driver().id(), vehicleB)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
    }

    @Test
    void create_whenAssigneeMayNotDrive_return422ButOwnerWithCanDriveWorks() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        long other = createVehicle(store.owner(), "Xe 2", 40, "BOX");
        TestUser owner = users.withCanDrive(store.owner(), true);

        create(owner, draft("P", "[]", assignment(store.dispatcher().id(), vehicle)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
        create(owner, draft("P", "[]", assignment(owner.id(), vehicle)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.routes[0].driver.role").value("OWNER"));
        create(owner, draft("P", "[]", assignment(store.driver().id(), vehicle) + "," + assignment(store.driver().id(), other)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
        create(owner, draft("P", "[]", assignment(store.driver().id(), vehicle) + "," + assignment(owner.id(), vehicle)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
    }

    @Test
    void create_whenVehicleInactiveOrDriverDisabled_return422InvalidAssignment() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        long inactive = createVehicle(store.owner(), "Xe 2", 40, "BOX");
        jdbcTemplate.update("UPDATE vehicles SET active = FALSE WHERE id = ?", inactive);
        TestUser disabled = users.addMember(store.id(), UserRole.DRIVER, false);
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED' WHERE id = ?", disabled.id());

        create(store.owner(), draft("P", "[]", assignment(store.driver().id(), inactive)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
        create(store.owner(), draft("P", "[]", assignment(disabled.id(), vehicle)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_ASSIGNMENT"));
    }

    @Test
    void create_whenStopOrAssignmentInvalid_return400WithDetail() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");

        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"timeWindow\":{\"start\":\"11:00\",\"end\":\"11:00\"}}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("stops[0].timeWindow.ordered"))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_TIME_WINDOW"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"  \"}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'stops[0].located')].code").value("ADDRESS_OR_COORDINATES_REQUIRED"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"lat\":16.0}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'stops[0].coordinatesComplete')].code").value("COORDINATES_INCOMPLETE"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"demands\":{\"ORDER\":1}}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("ORDER_DEMAND_NOT_ALLOWED"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"demands\":{\"BOX\":-1}}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("INVALID_DEMAND"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"demands\":{\"LITER\":1}}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"timeWindow\":{\"start\":\"09:00:30\",\"end\":\"10:00\"}}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\",\"recipientPhone\":\"123\"}]", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("stops[0].recipientPhone"));
        create(store.owner(), draft("P", "[]", """
                {"driverId":%d,"vehicleId":%d,"shiftStart":"17:00","shiftEnd":"08:00","maxDistanceMeters":0}"""
                .formatted(store.driver().id(), vehicle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'assignments[0].shiftOrdered')].code").value("INVALID_SHIFT"))
                .andExpect(jsonPath("$.details[?(@.field == 'assignments[0].maxDistanceMeters')].code").value("INVALID_MAX_DISTANCE"));
    }

    @Test
    void create_whenOverSizeLimit_return422PlanTooLarge() throws Exception {
        TestStore store = storeWithDepot();
        String stops = IntStream.range(0, 101)
                .mapToObj(i -> "{\"label\":\"S" + i + "\",\"lat\":16.0,\"lon\":108.0}")
                .collect(Collectors.joining(",", "[", "]"));

        create(store.owner(), draft("P", stops, ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_TOO_LARGE"));
    }

    @Test
    void replace_whenStopUnchanged_keepGeocodingResultAndReplaceTheRest() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        String body = create(store.owner(), draft("P", """
                [{"label":"Giữ","address":"12 Lê Duẩn"},{"label":"Đổi","address":"1 Bạch Đằng"},
                 {"label":"Bỏ","address":"5 Trần Phú"},{"label":"Toạ độ","lat":16.1,"lon":108.1}]""",
                assignment(store.driver().id(), vehicle)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long kept = stopId(body, 0);
        long changed = stopId(body, 1);
        long provided = stopId(body, 3);
        // What the geocoding job (T29) will write.
        jdbcTemplate.update("UPDATE route_plan_stops SET geocode_status = 'RESOLVED', lat = 16.07, lon = 108.22, "
                + "geocoded_display_name = '12 Lê Duẩn, Hải Châu' WHERE id IN (?, ?)", kept, changed);

        replace(store.owner(), planId, 0, draft("P mới", """
                [{"id":%d,"label":"Giữ (sửa nhãn)","address":"12 Lê Duẩn"},{"id":%d,"label":"Đổi","address":"2 Bạch Đằng"},
                 {"id":%d,"label":"Toạ độ","lat":16.1,"lon":108.1},{"label":"Mới","address":"9 Hùng Vương"}]"""
                .formatted(kept, changed, provided), assignment(store.driver().id(), vehicle)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("P mới"))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.routes", hasSize(1)))
                .andExpect(jsonPath("$.data.unassignedStops[*].label", contains("Giữ (sửa nhãn)", "Đổi", "Toạ độ", "Mới")))
                .andExpect(jsonPath("$.data.unassignedStops[0].id").value(kept))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].lat").value(16.07))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodedDisplayName").value("12 Lê Duẩn, Hải Châu"))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodeStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.unassignedStops[1].lat").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[1].geocodedDisplayName").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[2].geocodeStatus").value("PROVIDED"))
                .andExpect(jsonPath("$.data.unassignedStops[3].geocodeStatus").value("PENDING"));
    }

    @Test
    void replace_whenOptimized_clearResultAndRecreateRoutes() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        long other = createVehicle(store.owner(), "Xe 2", 10, "KG");
        String body = create(store.owner(), draft("P", "[{\"label\":\"A\",\"lat\":16.1,\"lon\":108.1,\"demands\":{\"BOX\":3}}]",
                assignment(store.driver().id(), vehicle))).andReturn().getResponse().getContentAsString();
        long planId = id(body);
        optimize(planId, routeId(body, 0), stopId(body, 0));

        mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.optimizedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.routes[0].load").value(3))
                .andExpect(jsonPath("$.data.routes[0].stops[0].sequence").value(1))
                .andExpect(jsonPath("$.data.routes[0].stops[0].dropReason").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops", hasSize(0)));

        // Same driver with another vehicle: the unique (plan, driver) key must not block the new route.
        replace(store.owner(), planId, 0, draft("P", "[{\"id\":%d,\"label\":\"A\",\"lat\":16.1,\"lon\":108.1}]"
                .formatted(stopId(body, 0)), assignment(store.driver().id(), other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.optimizedAt").value(nullValue()))
                .andExpect(jsonPath("$.data.totalDistanceMeters").value(nullValue()))
                .andExpect(jsonPath("$.data.routes[0].vehicle.capacityUnit").value("KG"))
                .andExpect(jsonPath("$.data.routes[0].plannedDistanceMeters").value(nullValue()))
                .andExpect(jsonPath("$.data.routes[0].stops", hasSize(0)))
                .andExpect(jsonPath("$.data.unassignedStops[0].sequence").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[0].demands.BOX").doesNotExist())
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("NOT_OPTIMIZED"));
    }

    @Test
    void get_whenOptimizedWithDroppedStop_reportInfeasibleAndOrderLoad() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 5, "ORDER");
        String body = create(store.owner(), draft("P", """
                [{"label":"A","lat":16.1,"lon":108.1},{"label":"B","lat":16.2,"lon":108.2}]""",
                assignment(store.driver().id(), vehicle))).andReturn().getResponse().getContentAsString();
        optimize(id(body), routeId(body, 0), stopId(body, 0));

        mockMvc.perform(get(PLANS + "/" + id(body)).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.routes[0].load").value(1))
                .andExpect(jsonPath("$.data.unassignedStops[0].label").value("B"))
                .andExpect(jsonPath("$.data.unassignedStops[0].dropReason").value("INFEASIBLE"));
    }

    @Test
    void get_whenDriverDisabledAndVehicleRenamedSinceAssignment_showCurrentNameAndSnapshotCapacity() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        long planId = id(create(store.owner(), draft("P", "[]", assignment(store.driver().id(), vehicle)))
                .andReturn().getResponse().getContentAsString());
        jdbcTemplate.update("UPDATE users SET status = 'DISABLED', full_name = 'Tùng' WHERE id = ?", store.driver().id());
        jdbcTemplate.update("UPDATE vehicles SET name = 'Xe 1 mới', capacity = 10, capacity_unit = 'KG', active = FALSE WHERE id = ?", vehicle);

        mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer()))
                .andExpect(jsonPath("$.data.routes[0].driver.fullName").value("Tùng"))
                .andExpect(jsonPath("$.data.routes[0].driver.role").value("DRIVER"))
                .andExpect(jsonPath("$.data.routes[0].vehicle.name").value("Xe 1 mới"))
                .andExpect(jsonPath("$.data.routes[0].vehicle.capacity").value(40))
                .andExpect(jsonPath("$.data.routes[0].vehicle.capacityUnit").value("BOX"));
    }

    @Test
    void replace_whenVersionStaleOrMissing_return409Or400() throws Exception {
        TestStore store = storeWithDepot();
        long planId = id(create(store.owner(), draft("P", "[]", "")).andReturn().getResponse().getContentAsString());
        replace(store.owner(), planId, 0, draft("P1", "[]", "")).andExpect(status().isOk());

        replace(store.owner(), planId, 0, draft("P2", "[]", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));
        mockMvc.perform(put(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(draft("P2", "[]", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("version"))
                .andExpect(jsonPath("$.details[0].code").value("VERSION_REQUIRED"));
    }

    @Test
    void replace_whenStopIdNotInPlanOrRepeated_return400InvalidStopId() throws Exception {
        TestStore store = storeWithDepot();
        String first = create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        String second = create(store.owner(), draft("Q", "[{\"label\":\"B\",\"address\":\"Y\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        long own = stopId(first, 0);

        replace(store.owner(), id(first), 0, draft("P", """
                [{"id":%d,"label":"A","address":"X"},{"id":%d,"label":"A","address":"X"},{"id":%d,"label":"B","address":"Y"}]"""
                .formatted(own, own, stopId(second, 0)), ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[*].field", contains("stops[1].id", "stops[2].id")))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_STOP_ID"));
    }

    @Test
    void replace_whenBusyOrNotDraft_return409Or422() throws Exception {
        TestStore store = storeWithDepot();
        long planId = id(create(store.owner(), draft("P", "[]", "")).andReturn().getResponse().getContentAsString());

        jdbcTemplate.update("UPDATE route_plans SET processing_status = 'GEOCODING' WHERE id = ?", planId);
        replace(store.owner(), planId, 0, draft("P", "[]", ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
        jdbcTemplate.update("UPDATE route_plans SET processing_status = 'IDLE', status = 'PUBLISHED' WHERE id = ?", planId);
        replace(store.owner(), planId, 0, draft("P", "[]", ""))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_EDITABLE"));
    }

    @Test
    void patchStop_whenFieldsAbsentOrNull_keepOrClearThem() throws Exception {
        TestStore store = storeWithDepot();
        String body = create(store.owner(), draft("P", """
                [{"label":"A","address":"12 Lê Duẩn","recipientName":"Lan","demands":{"BOX":2},
                  "serviceTimeSeconds":120,"timeWindow":{"start":"09:00","end":"10:00"},"note":"n"}]""", ""))
                .andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long stop = stopId(body, 0);

        patch(store.owner(), planId, stop, "{\"version\":0,\"timeWindow\":null,\"demands\":null,\"note\":\"mới\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.unassignedStops[0].timeWindow").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[0].demands.BOX").doesNotExist())
                .andExpect(jsonPath("$.data.unassignedStops[0].note").value("mới"))
                .andExpect(jsonPath("$.data.unassignedStops[0].recipientName").value("Lan"))
                .andExpect(jsonPath("$.data.unassignedStops[0].serviceTimeSeconds").value(120))
                .andExpect(jsonPath("$.data.unassignedStops[0].address").value("12 Lê Duẩn"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PENDING"));
        patch(store.owner(), planId, stop, "{\"version\":1,\"lat\":16.06,\"lon\":108.22}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PROVIDED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].lat").value(16.06))
                .andExpect(jsonPath("$.data.unassignedStops[0].address").value("12 Lê Duẩn"));
        patch(store.owner(), planId, stop, "{\"version\":2,\"lat\":null,\"lon\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.unassignedStops[0].lat").value(nullValue()));
        patch(store.owner(), planId, stop, "{\"version\":3,\"address\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("ADDRESS_OR_COORDINATES_REQUIRED"));
        patch(store.owner(), planId, stop, "{\"version\":3,\"label\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("STOP_LABEL_REQUIRED"));
        patch(store.owner(), planId, stop, "{\"version\":3,\"lat\":16.0}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("COORDINATES_INCOMPLETE"));
        patch(store.owner(), planId, stop, "{\"version\":2,\"note\":\"x\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PLAN_MODIFIED"));
    }

    @Test
    void patchStop_whenOnlyAddressChanges_geocodeAgainButKeepUnchangedResult() throws Exception {
        TestStore store = storeWithDepot();
        String body = create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"12 Lê Duẩn\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long stop = stopId(body, 0);
        jdbcTemplate.update("UPDATE route_plan_stops SET geocode_status = 'FAILED', geocode_error = 'NOT_FOUND' WHERE id = ?", stop);

        patch(store.owner(), planId, stop, "{\"version\":0,\"address\":\"12 Lê Duẩn\",\"label\":\"B\"}")
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("FAILED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeError").value("NOT_FOUND"));
        patch(store.owner(), planId, stop, "{\"version\":1,\"address\":\"12 Lê Duẩn, Đà Nẵng\"}")
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeError").value(nullValue()));
    }

    @Test
    void patchStop_whenAddressCleared_keepCoordinatesAsProvided() throws Exception {
        TestStore store = storeWithDepot();
        String body = create(store.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"12 Lê Duẩn\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        long stop = stopId(body, 0);
        jdbcTemplate.update("UPDATE route_plan_stops SET geocode_status = 'RESOLVED', lat = 16.07, lon = 108.22, "
                + "geocoded_display_name = '12 Lê Duẩn, Hải Châu' WHERE id = ?", stop);

        patch(store.owner(), id(body), stop, "{\"version\":0,\"address\":null}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unassignedStops[0].address").value(nullValue()))
                .andExpect(jsonPath("$.data.unassignedStops[0].lat").value(16.07))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodeStatus").value("PROVIDED"))
                .andExpect(jsonPath("$.data.unassignedStops[0].geocodedDisplayName").value(nullValue()));
    }

    @Test
    void plan_whenOfAnotherStore_return404AndNeverListed() throws Exception {
        TestStore storeA = storeWithDepot();
        TestStore storeB = storeWithDepot();
        String body = create(storeA.owner(), draft("P", "[{\"label\":\"A\",\"address\":\"X\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long stop = stopId(body, 0);

        mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, storeB.dispatcher().bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        replace(storeB.owner(), planId, 0, draft("Stolen", "[]", "")).andExpect(status().isNotFound());
        patch(storeB.owner(), planId, stop, "{\"version\":0,\"note\":\"x\"}").andExpect(status().isNotFound());
        list(storeB.owner(), "").andExpect(jsonPath("$.data.items", hasSize(0)));
        list(storeA.owner(), "").andExpect(jsonPath("$.data.items[0].id").value(planId));

        String other = create(storeA.owner(), draft("Q", "[{\"label\":\"B\",\"address\":\"Y\"}]", ""))
                .andReturn().getResponse().getContentAsString();
        patch(storeA.owner(), planId, stopId(other, 0), "{\"version\":0,\"note\":\"x\"}").andExpect(status().isNotFound());
    }

    @Test
    void list_whenFiltered_returnSummariesNewestFirst() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 40, "BOX");
        create(store.owner(), draftOn("Thứ hai", "2026-11-02", "[{\"label\":\"A\",\"address\":\"X\"},{\"label\":\"B\",\"address\":\"Y\"}]",
                assignment(store.driver().id(), vehicle))).andExpect(status().isCreated());
        create(store.owner(), draftOn("Thứ ba", "2026-11-03", "[]", "")).andExpect(status().isCreated());
        long cancelled = id(create(store.owner(), draftOn("Huỷ", "2026-11-03", "[]", ""))
                .andReturn().getResponse().getContentAsString());
        jdbcTemplate.update("UPDATE route_plans SET status = 'CANCELLED' WHERE id = ?", cancelled);

        list(store.dispatcher(), "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.items[*].name", contains("Huỷ", "Thứ ba", "Thứ hai")))
                .andExpect(jsonPath("$.data.items[2].stopCount").value(2))
                .andExpect(jsonPath("$.data.items[2].routeCount").value(1))
                .andExpect(jsonPath("$.data.items[1].stopCount").value(0))
                .andExpect(jsonPath("$.data.items[2].processingStatus").value("IDLE"));
        list(store.dispatcher(), "?date=2026-11-03&status=DRAFT")
                .andExpect(jsonPath("$.data.items[*].name", contains("Thứ ba")));
        list(store.dispatcher(), "?sort=name,asc&size=2")
                .andExpect(jsonPath("$.data.items[*].name", contains("Huỷ", "Thứ ba")))
                .andExpect(jsonPath("$.data.totalPages").value(2));
        list(store.dispatcher(), "?sort=storeId")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SORT_FIELD"));
        list(store.dispatcher(), "?status=UNKNOWN")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void plans_whenDriverOrAdmin_return403() throws Exception {
        TestStore store = storeWithDepot();
        TestUser admin = users.createAdmin();
        long planId = id(create(store.owner(), draft("P", "[]", "")).andReturn().getResponse().getContentAsString());

        for (TestUser caller : new TestUser[]{store.driver(), admin}) {
            list(caller, "").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
            create(caller, draft("P", "[]", "")).andExpect(status().isForbidden());
            mockMvc.perform(get(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, caller.bearer()))
                    .andExpect(status().isForbidden());
            replace(caller, planId, 0, draft("P", "[]", "")).andExpect(status().isForbidden());
        }
    }

    private TestStore storeWithDepot() throws Exception {
        TestStore store = users.createStore();
        mockMvc.perform(put("/api/v1/store").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shop\",\"depotAddress\":\"Kho Hải Châu\",\"depotLat\":16.05,\"depotLon\":108.2,"
                                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}"))
                .andExpect(status().isOk());
        return store;
    }

    private long createVehicle(TestUser caller, String name, int capacity, String unit) throws Exception {
        String body = mockMvc.perform(post("/api/v1/vehicles").header(HttpHeaders.AUTHORIZATION, caller.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"capacity\":%d,\"capacityUnit\":\"%s\"}".formatted(name, capacity, unit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return id(body);
    }

    /**
     * What the optimizer (T25) will write: the given stop on the given route, the other stops dropped.
     */
    private void optimize(long planId, long routeId, long stopId) {
        jdbcTemplate.update("UPDATE route_plans SET optimized_at = UTC_TIMESTAMP(6), total_distance_m = 1000, "
                + "total_duration_s = 600 WHERE id = ?", planId);
        jdbcTemplate.update("UPDATE route_plan_routes SET planned_distance_m = 1000, planned_duration_s = 600 WHERE id = ?", routeId);
        jdbcTemplate.update("UPDATE route_plan_stops SET route_id = ?, sequence = 1, planned_arrival = UTC_TIMESTAMP(6) "
                + "WHERE id = ?", routeId, stopId);
    }

    private static String draft(String name, String stops, String assignments) {
        return draftOn(name, "2026-11-02", stops, assignments);
    }

    private static String draftOn(String name, String planDate, String stops, String assignments) {
        return "{\"name\":\"%s\",\"planDate\":\"%s\",\"stops\":%s,\"assignments\":[%s]}".formatted(name, planDate, stops, assignments);
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"08:00\",\"shiftEnd\":\"17:00\",\"maxDistanceMeters\":80000}"
                .formatted(driverId, vehicleId);
    }

    private ResultActions create(TestUser caller, String body) throws Exception {
        return mockMvc.perform(post(PLANS).header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions replace(TestUser caller, long planId, long version, String draft) throws Exception {
        return mockMvc.perform(put(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "," + draft.substring(1)));
    }

    private ResultActions patch(TestUser caller, long planId, long stopId, String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch(PLANS + "/" + planId + "/stops/" + stopId).header(HttpHeaders.AUTHORIZATION, caller.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions list(TestUser caller, String query) throws Exception {
        return mockMvc.perform(get(PLANS + query).header(HttpHeaders.AUTHORIZATION, caller.bearer()));
    }

    private static long id(String body) {
        return ((Number) JsonPath.read(body, "$.data.id")).longValue();
    }

    private static long stopId(String body, int index) {
        return ((Number) JsonPath.read(body, "$.data.unassignedStops[" + index + "].id")).longValue();
    }

    private static long routeId(String body, int index) {
        return ((Number) JsonPath.read(body, "$.data.routes[" + index + "].id")).longValue();
    }
}
