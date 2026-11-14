package uk.huy.pathwise.acceptance;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T46, P-48: the generated OpenAPI document says which response fields are always there and which may be {@code null},
 * and real responses keep to it. Each body is checked against the schema the document gives for that endpoint and
 * status: every {@code required} field is present, and {@code null} appears only where the type allows it.
 * The bodies are the samples in {@code docs/ux/samples} (written by {@code MvpScenarioIntegrationTest} from a full
 * scenario) plus the account endpoints the scenario does not record.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class OpenApiNullabilityIntegrationTest extends AbstractIntegrationTest {
    private static final Path SAMPLES = Path.of("..", "docs", "ux", "samples");
    /** Sample file → the endpoint that returned it (MvpScenarioIntegrationTest). */
    private static final Map<String, String> SAMPLE_ENDPOINTS = Map.ofEntries(
            Map.entry("plan-detail-geocoded", "get /api/v1/plans/{id} 200"),
            Map.entry("plan-detail-optimized", "get /api/v1/plans/{id} 200"),
            Map.entry("plan-detail-completed", "get /api/v1/plans/{id} 200"),
            Map.entry("job-optimize", "post /api/v1/plans/{id}/optimize 202"),
            Map.entry("plan-list", "get /api/v1/plans 200"),
            Map.entry("vehicle-list", "get /api/v1/vehicles 200"),
            Map.entry("member-list", "get /api/v1/store/members 200"),
            Map.entry("invitation-list", "get /api/v1/store/invitations 200"),
            Map.entry("store", "get /api/v1/store 200"),
            Map.entry("user-me", "get /api/v1/users/me 200"),
            Map.entry("driver-route-list", "get /api/v1/driver/routes 200"),
            Map.entry("driver-route-detail", "get /api/v1/driver/routes/{routeId} 200"),
            Map.entry("driver-stop-status-update", "patch /api/v1/driver/routes/{routeId}/stops/{stopId}/status 200"),
            Map.entry("plan-driver-locations", "get /api/v1/plans/{id}/driver-locations 200"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    private Object openApi;

    @BeforeEach
    void loadOpenApi() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        openApi = JsonPath.parse(document).json();
    }

    @Test
    void openApi_marksRequiredAndNullableFields() {
        Map<String, Object> user = schema("GetUserResponse");
        assertThat(list(user.get("required")))
                .containsExactlyInAnyOrder("id", "email", "fullName", "phoneNumber", "role", "storeId", "status", "canDrive",
                        "storeStatus", "storeLockReason");
        assertThat(types(property(user, "fullName"))).containsExactlyInAnyOrder("string", "null");
        assertThat(types(property(user, "storeLockReason"))).containsExactlyInAnyOrder("string", "null");
        assertThat(types(property(user, "email"))).containsExactly("string");

        // NON_NULL: a missing value is left out, so the field is not required (API_SPEC §2).
        Map<String, Object> authUser = schema("AuthUserResponse");
        assertThat(list(authUser.get("required"))).containsExactlyInAnyOrder("id", "email", "role", "canDrive");
        assertThat(list(property(authUser, "role").get("enum"))).containsExactly("ADMIN", "OWNER", "DISPATCHER", "DRIVER");

        // A nullable reference: anyOf the schema and null.
        assertThat(property(schema("StoreResponse"), "depot")).containsKey("anyOf");

        // Merge patch: null clears the value (API_SPEC §1); required stays as validation says.
        assertThat(types(property(schema("UpdateProfileRequest"), "fullName"))).contains("null");
        assertThat(types(property(schema("PatchStopRequest"), "label"))).doesNotContain("null");
        assertThat(property(schema("PatchStopRequest"), "timeWindow")).containsKey("anyOf");

        // A constraint of another validation group only binds some endpoints: version is PUT only (F-51).
        assertThat(list(schema("PlanDraftRequest").get("required"))).containsExactlyInAnyOrder("name", "planDate");
    }

    @Test
    void openApi_declaresSuccessStatusOfApiSpec() {
        assertThat(responses("post", "/api/v1/auth/register")).containsOnlyKeys("201");
        assertThat(responses("post", "/api/v1/auth/email/verify")).containsOnlyKeys("204");
        assertThat(responses("delete", "/api/v1/auth/sessions/{sessionId}")).containsOnlyKeys("204");
        assertThat(responses("post", "/api/v1/plans/{id}/optimize")).containsOnlyKeys("202");
        assertThat(responses("get", "/api/v1/plans/{id}")).containsOnlyKeys("200");
        assertThat(responses("post", "/api/v1/driver/location")).containsOnlyKeys("204");
        assertThat(responses("put", "/api/v1/driver/location/sharing")).containsOnlyKeys("204");
    }

    @Test
    void samples_keepToTheirSchema() throws Exception {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, String> sample : SAMPLE_ENDPOINTS.entrySet()) {
            String body = Files.readString(SAMPLES.resolve(sample.getKey() + ".json"), StandardCharsets.UTF_8);
            String[] endpoint = sample.getValue().split(" ");
            check(sample.getKey(), body, endpoint[0], endpoint[1], endpoint[2], errors);
        }
        assertThat(errors).isEmpty();
    }

    @Test
    void accountResponses_keepToTheirSchema() throws Exception {
        TestUsers.TestStore store = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender).createStore();
        String owner = store.owner().bearer();
        List<String> errors = new ArrayList<>();

        String logIn = "{\"email\":\"" + store.owner().email() + "\",\"password\":\"" + TestUsers.PASSWORD + "\"}";
        check("log-in web", body(post("/api/v1/auth/log-in").contentType(MediaType.APPLICATION_JSON).content(logIn), null, 200),
                "post", "/api/v1/auth/log-in", "200", errors);
        check("log-in mobile", body(post("/api/v1/auth/log-in").header("X-Client-Platform", "mobile")
                        .contentType(MediaType.APPLICATION_JSON).content(logIn), null, 200),
                "post", "/api/v1/auth/log-in", "200", errors);
        check("sessions", body(get("/api/v1/auth/sessions"), owner, 200), "get", "/api/v1/auth/sessions", "200", errors);
        check("users/me", body(get("/api/v1/users/me"), owner, 200), "get", "/api/v1/users/me", "200", errors);
        check("store without depot", body(get("/api/v1/store"), owner, 200), "get", "/api/v1/store", "200", errors);
        check("vehicle without plate", body(post("/api/v1/vehicles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Xe 1\",\"capacity\":30,\"capacityUnit\":\"KG\"}"), owner, 201),
                "post", "/api/v1/vehicles", "201", errors);
        check("invitation", body(post("/api/v1/store/invitations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"moi-" + store.id() + "@pathwise.test\",\"role\":\"DRIVER\"}"), owner, 201),
                "post", "/api/v1/store/invitations", "201", errors);
        assertThat(errors).isEmpty();
    }

    private String body(MockHttpServletRequestBuilder request, String bearer, int expected) throws Exception {
        if (bearer != null) request.header(HttpHeaders.AUTHORIZATION, bearer);
        MvcResult result = mockMvc.perform(request).andExpect(status().is(expected)).andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private void check(String name, String body, String method, String path, String status, List<String> errors) {
        Map<String, Object> response = map(map(responses(method, path).get(status)).get("content"));
        Map<String, Object> schema = map(map(response.get("application/json")).get("schema"));
        validate(JsonPath.parse(body).json(), schema, name, errors);
    }

    /** Required fields present, null only where allowed; follows references, anyOf, arrays and maps. */
    private void validate(Object value, Map<String, Object> schema, String path, List<String> errors) {
        if (schema.get("$ref") instanceof String ref) {
            validate(value, schema(ref.substring(ref.lastIndexOf('/') + 1)), path, errors);
            return;
        }
        if (schema.get("anyOf") instanceof List<?> options) {
            if (value == null) {
                if (options.stream().noneMatch(option -> types(map(option)).contains("null"))) errors.add(path + " is null");
                return;
            }
            options.stream().map(OpenApiNullabilityIntegrationTest::map).filter(option -> !types(option).contains("null"))
                    .findFirst().ifPresent(option -> validate(value, option, path, errors));
            return;
        }
        if (value == null) {
            if (!types(schema).isEmpty() && !types(schema).contains("null")) errors.add(path + " is null");
            return;
        }
        if (value instanceof Map<?, ?> object) {
            for (Object field : list(schema.get("required"))) {
                if (!object.containsKey(field)) errors.add(path + "." + field + " is missing");
            }
            Map<String, Object> properties = schema.get("properties") == null ? Map.of() : map(schema.get("properties"));
            object.forEach((key, item) -> {
                Object property = properties.get(String.valueOf(key));
                if (property != null) validate(item, map(property), path + "." + key, errors);
                else if (schema.get("additionalProperties") instanceof Map<?, ?> values) {
                    validate(item, map(values), path + "." + key, errors);
                }
            });
        } else if (value instanceof List<?> items && schema.get("items") instanceof Map<?, ?> itemSchema) {
            for (int i = 0; i < items.size(); i++) validate(items.get(i), map(itemSchema), path + "[" + i + "]", errors);
        }
    }

    private Map<String, Object> schema(String name) {
        return map(JsonPath.read(openApi, "$.components.schemas['" + name + "']"));
    }

    private Map<String, Object> responses(String method, String path) {
        return map(JsonPath.read(openApi, "$.paths['" + path + "']." + method + ".responses"));
    }

    private static Map<String, Object> property(Map<String, Object> schema, String name) {
        return map(map(schema.get("properties")).get(name));
    }

    private static List<String> types(Map<?, ?> schema) {
        Object type = schema.get("type");
        if (type instanceof String single) return List.of(single);
        if (type instanceof Collection<?> many) return many.stream().map(Objects::toString).toList();
        return List.of();
    }

    private static List<Object> list(Object value) {
        return value instanceof List<?> items ? new ArrayList<>(items) : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) Objects.requireNonNull(value);
    }
}
