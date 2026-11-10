package uk.huy.pathwise.acceptance;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.FakeGeocodingProvider;
import uk.huy.pathwise.support.TestAccounts;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T43: the end-to-end scenario of REQUIREMENTS.md §6 (criterion 2), through the public API only, from an OWNER who
 * registers to a COMPLETED plan. Geocoding and travel matrix are fakes; the solver is the real OR-Tools.
 * <p>
 * Responses at the main steps are written to {@code target/ux-samples/} as realistic sample data for client design
 * (docs/ux/samples is a snapshot of them).
 */
@AutoConfigureMockMvc
class MvpScenarioIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern INVITATION_CODE = Pattern.compile(">([A-Za-z0-9_-]{43})<");
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final String PASSWORD = "Passw0rd!x";
    private static final String DAY = "2026-12-01";
    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(17, 0);
    private static final int STOPS = 24;
    // Address only and unknown to the geocoder: fails, then the dispatcher gives coordinates.
    private static final int UNKNOWN_ADDRESS = 7;
    // Time window after every shift: the solver cannot place it.
    private static final int INFEASIBLE = 23;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private FakeGeocodingProvider geocoder;

    @Test
    void mvpScenario_fromRegistrationToCompletedPlan() throws Exception {
        String run = UUID.randomUUID().toString();

        // OWNER registers, verifies the email, logs in and sets the depot.
        String ownerEmail = "owner-" + run + "@pathwise.test";
        TestAccounts.register(mockMvc, ownerEmail, PASSWORD, "Tiệm E2E");
        call(post("/api/v1/auth/email/verify"), null,
                "{\"email\":\"" + ownerEmail + "\",\"otp\":\"" + TestAccounts.lastOtp(emailSender, ownerEmail) + "\"}")
                .andExpect(status().isNoContent());
        String owner = logIn(ownerEmail);
        call(put("/api/v1/store"), owner, "{\"name\":\"Tiệm E2E\",\"depot\":{\"address\":\"Kho 45 Lê Lợi, Hải Châu, Đà Nẵng\",\"lat\":16.05,\"lon\":108.2},"
                + "\"timezone\":\"Asia/Ho_Chi_Minh\"}").andExpect(status().isOk());

        // One DISPATCHER and two DRIVERs are invited and accept.
        String dispatcher = invitedMember(owner, "dispatcher-" + run + "@pathwise.test", "DISPATCHER", "Điều phối");
        String driverAEmail = "driver-a-" + run + "@pathwise.test";
        String driverBEmail = "driver-b-" + run + "@pathwise.test";
        String driverA = invitedMember(owner, driverAEmail, "DRIVER", "Tài xế A");
        String driverB = invitedMember(owner, driverBEmail, "DRIVER", "Tài xế B");
        long driverAId = userId(driverA);
        long driverBId = userId(driverB);

        // The DISPATCHER creates two vehicles; together they carry every stop, one alone does not.
        long vehicle1 = id(call(post("/api/v1/vehicles"), dispatcher, "{\"name\":\"Xe 1\",\"capacity\":30,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isCreated()));
        long vehicle2 = id(call(post("/api/v1/vehicles"), dispatcher, "{\"name\":\"Xe 2\",\"capacity\":30,\"capacityUnit\":\"BOX\"}")
                .andExpect(status().isCreated()));

        // A plan of 24 stops, most with an address only, some with time windows; geocoding runs in the background.
        long planId = id(call(post("/api/v1/plans"), dispatcher, planDraft(run, driverAId, vehicle1, driverBId, vehicle2))
                .andExpect(status().isCreated()));
        DocumentContext plan = plan(dispatcher, planId);
        sample("plan-detail-geocoded", plan);
        assertThat((String) plan.read("$.processingStatus")).isEqualTo("IDLE");
        assertThat((Integer) plan.read("$.geocodingSummary.failed")).isEqualTo(1);
        assertThat((Integer) plan.read("$.geocodingSummary.resolved")).isEqualTo(19);
        assertThat((Integer) plan.read("$.geocodingSummary.provided")).isEqualTo(4);

        // The DISPATCHER fixes the stop that failed with coordinates, then optimizes.
        List<Map<String, Object>> failed = plan.read("$.unassignedStops[?(@.geocodeStatus == 'FAILED')]");
        assertThat(failed).hasSize(1);
        assertThat(failed.getFirst().get("geocodeError")).isEqualTo("NOT_FOUND");
        call(patch("/api/v1/plans/" + planId + "/stops/" + failed.getFirst().get("id")), dispatcher,
                "{\"version\":" + plan.read("$.version") + ",\"lat\":16.061,\"lon\":108.215}").andExpect(status().isOk());
        plan = plan(dispatcher, planId);
        assertThat((Integer) plan.read("$.geocodingSummary.failed")).isZero();
        String job = call(post("/api/v1/plans/" + planId + "/optimize"), dispatcher, "{\"version\":" + plan.read("$.version") + "}")
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(job, "$.data.status")).isEqualTo("SUCCEEDED");
        sample("job-optimize", job);

        // The result respects capacity, time windows and shifts, and lists the stop it could not place.
        plan = plan(dispatcher, planId);
        sample("plan-detail-optimized", plan);
        assertResultRespectsConstraints(plan);

        // Publish; each DRIVER sees exactly their own route with coordinates for the map link.
        call(post("/api/v1/plans/" + planId + "/publish"), dispatcher, "{\"version\":" + plan.read("$.version") + "}")
                .andExpect(status().isOk());
        plan = plan(dispatcher, planId);
        long publishedVersion = ((Number) plan.read("$.version")).longValue();
        List<Long> stopsOfA = assertDriverSeesOwnRoute(driverA, driverAId, plan);
        List<Long> stopsOfB = assertDriverSeesOwnRoute(driverB, driverBId, plan);

        // Drivers report their stops: the plan moves to IN_PROGRESS, then COMPLETED.
        long routeA = driverRouteId(driverA);
        long routeB = driverRouteId(driverB);

        // Driver A's app sends a batch of positions; driver B chooses not to share (T32, P-53). The dispatcher sees
        // A's newest point and B's reason.
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        call(post("/api/v1/driver/location"), driverA, "{\"points\":["
                + point(16.0601, 108.1979, now.minusSeconds(60)) + "," + point(16.0605, 108.1985, now.minusSeconds(30)) + "]}")
                .andExpect(status().isNoContent());
        call(put("/api/v1/driver/location/sharing"), driverB, "{\"routeId\":" + routeB + ",\"state\":\"DECLINED\"}")
                .andExpect(status().isNoContent());
        String locations = body(get("/api/v1/plans/" + planId + "/driver-locations"), dispatcher);
        assertThat(JsonPath.<List<Integer>>read(locations, "$.data[*].routeId")).containsExactlyInAnyOrder((int) routeA, (int) routeB);
        assertThat(JsonPath.<List<Double>>read(locations, "$.data[?(@.routeId == " + routeA + ")].location.lat")).containsExactly(16.0605);
        assertThat(JsonPath.<List<String>>read(locations, "$.data[?(@.routeId == " + routeB + ")].sharing.state")).containsExactly("DECLINED");
        sample("plan-driver-locations", locations);
        String first = updateStop(driverA, routeA, stopsOfA.getFirst(), "{\"status\":\"DELIVERED\"}");
        assertThat((String) JsonPath.read(first, "$.data.planStatus")).isEqualTo("IN_PROGRESS");
        sample("driver-stop-status-update", first);
        assertThat((String) plan(dispatcher, planId).read("$.status")).isEqualTo("IN_PROGRESS");
        for (Long stop : stopsOfA.subList(1, stopsOfA.size())) updateStop(driverA, routeA, stop, "{\"status\":\"DELIVERED\"}");
        updateStop(driverB, routeB, stopsOfB.getFirst(), "{\"status\":\"FAILED\",\"failureReason\":\"Vắng nhà\"}");
        String last = null;
        for (Long stop : stopsOfB.subList(1, stopsOfB.size())) last = updateStop(driverB, routeB, stop, "{\"status\":\"SKIPPED\"}");
        assertThat((String) JsonPath.read(last, "$.data.planStatus")).isEqualTo("COMPLETED");

        // The DISPATCHER sees it on reload; status updates did not change the plan version.
        plan = plan(dispatcher, planId);
        assertThat((String) plan.read("$.status")).isEqualTo("COMPLETED");
        assertThat(((Number) plan.read("$.version")).longValue()).isEqualTo(publishedVersion);
        List<String> reasons = plan.read("$.routes[*].stops[?(@.status == 'FAILED')].failureReason");
        assertThat(reasons).containsExactly("Vắng nhà");
        sample("plan-detail-completed", plan);

        // More sample data for client design: lists and two error bodies.
        sample("plan-list", body(get("/api/v1/plans?date=" + DAY), dispatcher));
        sample("vehicle-list", body(get("/api/v1/vehicles"), dispatcher));
        sample("member-list", body(get("/api/v1/store/members"), owner));
        sample("invitation-list", body(get("/api/v1/store/invitations"), owner));
        sample("store", body(get("/api/v1/store"), owner));
        sample("user-me", body(get("/api/v1/users/me"), driverA));
        sample("error-plan-not-editable", call(post("/api/v1/plans/" + planId + "/publish"), dispatcher,
                "{\"version\":" + publishedVersion + "}").andExpect(status().isUnprocessableContent()).andReturn().getResponse().getContentAsString());
        sample("error-validation", call(post("/api/v1/vehicles"), dispatcher, "{\"name\":\"\",\"capacity\":0}")
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString());
    }

    private static final String[] STREETS = {"Hùng Vương, Hải Châu", "Trần Phú, Hải Châu", "Bạch Đằng, Hải Châu",
            "Nguyễn Văn Linh, Thanh Khê", "Lê Duẩn, Hải Châu", "Phan Châu Trinh, Hải Châu", "Ông Ích Khiêm, Thanh Khê",
            "Điện Biên Phủ, Thanh Khê", "Nguyễn Tri Phương, Thanh Khê", "Hoàng Diệu, Hải Châu"};
    private static final String[] RECIPIENTS = {"Chị Lan", "Anh Minh", "Cô Hạnh", "Anh Tuấn", "Chị Mai", "Bác Sơn",
            "Anh Khoa", "Chị Thảo"};

    /**
     * Realistic addresses for the sample data, each with this run's number so that no other test registers the same one
     * in the shared fake geocoder.
     */
    private String planDraft(String run, long driverA, long vehicleA, long driverB, long vehicleB) {
        int house = 100 + Math.floorMod(run.hashCode(), 800);
        List<String> stops = new ArrayList<>();
        for (int i = 0; i < STOPS; i++) {
            double lat = 16.04 + 0.004 * (i % 6);
            double lon = 108.19 + 0.006 * (i / 6);
            String location;
            if (i < 20) {
                String address = (house + i) + " " + STREETS[i % STREETS.length] + ", Đà Nẵng";
                if (i != UNKNOWN_ADDRESS) geocoder.resolve(address, lat, lon);
                location = "\"address\":\"" + address + "\"";
            } else {
                location = "\"lat\":" + lat + ",\"lon\":" + lon;
            }
            String window = i == INFEASIBLE ? ",\"timeWindow\":{\"start\":\"20:00\",\"end\":\"21:00\"}"
                    : i % 4 == 0 ? ",\"timeWindow\":{\"start\":\"09:00\",\"end\":\"11:00\"}"
                    : i % 4 == 1 ? ",\"timeWindow\":{\"start\":\"13:00\",\"end\":\"16:00\"}"
                    : "";
            String note = i % 5 == 2 ? ",\"note\":\"Gọi trước 10 phút\"" : "";
            stops.add("{\"label\":\"ĐH-" + (1001 + i) + "\",\"recipientName\":\"" + RECIPIENTS[i % RECIPIENTS.length]
                    + "\",\"recipientPhone\":\"0905123" + String.format("%03d", i) + "\"," + location
                    + ",\"demands\":{\"BOX\":2},\"serviceTimeSeconds\":120" + window + note + "}");
        }
        return "{\"name\":\"Giao sáng 01/12\",\"planDate\":\"" + DAY + "\",\"stops\":[" + String.join(",", stops) + "],\"assignments\":["
                + assignment(driverA, vehicleA) + "," + assignment(driverB, vehicleB) + "]}";
    }

    private static String assignment(long driverId, long vehicleId) {
        return "{\"driverId\":%d,\"vehicleId\":%d,\"shiftStart\":\"%s\",\"shiftEnd\":\"%s\"}"
                .formatted(driverId, vehicleId, SHIFT_START, SHIFT_END);
    }

    private static void assertResultRespectsConstraints(DocumentContext plan) {
        assertThat((String) plan.read("$.optimizedAt")).isNotNull();
        List<Map<String, Object>> unassigned = plan.read("$.unassignedStops");
        assertThat(unassigned).singleElement().satisfies(stop -> {
            assertThat(stop.get("label")).isEqualTo("ĐH-" + (1001 + INFEASIBLE));
            assertThat(stop.get("dropReason")).isEqualTo("INFEASIBLE");
        });
        List<Map<String, Object>> routes = plan.read("$.routes");
        assertThat(routes).hasSize(2);
        int assigned = 0;
        for (Map<String, Object> route : routes) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stops = (List<Map<String, Object>>) route.get("stops");
            @SuppressWarnings("unchecked")
            Map<String, Object> vehicle = (Map<String, Object>) route.get("vehicle");
            assertThat(stops).as("both vehicles are needed").isNotEmpty();
            assertThat((Integer) route.get("load")).isLessThanOrEqualTo((Integer) vehicle.get("capacity"))
                    .isEqualTo(2 * stops.size());
            assertThat(local(route.get("plannedStart"))).isAfterOrEqualTo(SHIFT_START);
            assertThat(local(route.get("plannedEnd"))).isBeforeOrEqualTo(SHIFT_END);
            for (Map<String, Object> stop : stops) {
                @SuppressWarnings("unchecked")
                Map<String, Object> window = (Map<String, Object>) stop.get("timeWindow");
                LocalTime arrival = local(stop.get("plannedArrival"));
                assertThat(arrival).isBetween(SHIFT_START, SHIFT_END);
                if (window != null) {
                    assertThat(arrival).as("arrival at %s", stop.get("label"))
                            .isBetween(LocalTime.parse((String) window.get("start")), LocalTime.parse((String) window.get("end")));
                }
            }
            assigned += stops.size();
        }
        assertThat(assigned).isEqualTo(STOPS - 1);
    }

    /**
     * @return the stop ids of the driver's route, in visiting order
     */
    private List<Long> assertDriverSeesOwnRoute(String driver, long driverId, DocumentContext plan) throws Exception {
        String list = call(get("/api/v1/driver/routes?date=" + DAY), driver, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        sample("driver-route-list", list);
        List<Integer> routeIds = JsonPath.read(list, "$.data[*].routeId");
        List<Integer> expected = plan.read("$.routes[?(@.driver.id == " + driverId + ")].id");
        assertThat(routeIds).isEqualTo(expected);

        String detail = call(get("/api/v1/driver/routes/" + routeIds.getFirst()), driver, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        sample("driver-route-detail", detail);
        List<Number> stopIds = JsonPath.read(detail, "$.data.stops[*].id");
        List<Number> dispatcherView = plan.read("$.routes[?(@.driver.id == " + driverId + ")].stops[*].id");
        assertThat(stopIds).isEqualTo(dispatcherView);
        // Enough for the app to build a Google/Apple Maps link to every stop (FR-D02).
        assertThat(JsonPath.<List<Object>>read(detail, "$.data.stops[*].lat")).hasSameSizeAs(stopIds).doesNotContainNull();
        assertThat(JsonPath.<List<Object>>read(detail, "$.data.stops[*].lon")).hasSameSizeAs(stopIds).doesNotContainNull();
        return stopIds.stream().map(Number::longValue).toList();
    }

    private long driverRouteId(String driver) throws Exception {
        String list = call(get("/api/v1/driver/routes?date=" + DAY), driver, null).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(list, "$.data[0].routeId")).longValue();
    }

    private static String point(double lat, double lon, Instant recordedAt) {
        return "{\"lat\":%s,\"lon\":%s,\"accuracyMeters\":12,\"headingDegrees\":90,\"speedMps\":7.8,\"recordedAt\":\"%s\"}"
                .formatted(lat, lon, recordedAt);
    }

    private String updateStop(String driver, long routeId, long stopId, String body) throws Exception {
        return call(patch("/api/v1/driver/routes/" + routeId + "/stops/" + stopId + "/status"), driver, body)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /**
     * Invites {@code email} with {@code role}, accepts with the code from the email and logs in.
     *
     * @return the bearer header of the new member
     */
    private String invitedMember(String owner, String email, String role, String fullName) throws Exception {
        call(post("/api/v1/store/invitations"), owner, "{\"email\":\"" + email + "\",\"role\":\"" + role + "\"}")
                .andExpect(status().isCreated());
        Matcher code = INVITATION_CODE.matcher(emailSender.sentTo(email).getLast().content());
        assertThat(code.find()).as("invitation code in the email").isTrue();
        call(post("/api/v1/invitations/accept"), null,
                "{\"code\":\"" + code.group(1) + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"" + fullName + "\"}")
                .andExpect(status().isNoContent());
        return logIn(email);
    }

    private String logIn(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/log-in").header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.data.accessToken");
    }

    private long userId(String bearer) throws Exception {
        return id(call(get("/api/v1/users/me"), bearer, null).andExpect(status().isOk()));
    }

    private DocumentContext plan(String bearer, long planId) throws Exception {
        String body = call(get("/api/v1/plans/" + planId), bearer, null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse((Object) JsonPath.read(body, "$.data"));
    }

    private String body(MockHttpServletRequestBuilder request, String bearer) throws Exception {
        return call(request, bearer, null).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private static void sample(String name, DocumentContext data) throws IOException {
        sample(name, "{\"data\":" + data.jsonString() + "}");
    }

    /**
     * Writes a response body as sample data; the second driver's route overwrites the first, which is fine.
     */
    private static void sample(String name, String body) throws IOException {
        Path directory = Path.of("target", "ux-samples");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(name + ".json"), body, StandardCharsets.UTF_8);
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String bearer, String body) throws Exception {
        if (bearer != null) request.header(HttpHeaders.AUTHORIZATION, bearer);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        return mockMvc.perform(request);
    }

    private static long id(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private static LocalTime local(Object instant) {
        return Instant.parse((String) instant).atZone(ZONE).toLocalTime();
    }
}
