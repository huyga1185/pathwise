package uk.huy.pathwise.acceptance;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T43, REQUIREMENTS.md §6 criterion 7: the endpoints of the OpenAPI document the application generates are exactly the
 * endpoints {@code docs/API_SPEC.md} lists as implemented (every row not marked ❌), by method and path. Path variable
 * names are ignored. Field-level differences are checked by hand (docs/ACCEPTANCE.md); the generated document is
 * written to {@code target/openapi.json} for that.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "springdoc.api-docs.enabled=true")
class ApiSpecContractIntegrationTest extends AbstractIntegrationTest {
    private static final Path API_SPEC = Path.of("..", "docs", "API_SPEC.md");
    private static final String API_PREFIX = "/api/v1";
    private static final Pattern SECTION = Pattern.compile("^## ");
    private static final Pattern SECTION_BASE = Pattern.compile("`(/api/v1[^`]*)`");
    private static final Pattern ENDPOINT_ROW = Pattern.compile("^\\| `(GET|POST|PUT|PATCH|DELETE) (/[^`]*)` \\|.*\\| ([^|]*) \\|$");
    private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "patch", "delete");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApi_matchesEndpointsImplementedInApiSpec() throws Exception {
        String openApi = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Files.writeString(Path.of("target", "openapi.json"), openApi, StandardCharsets.UTF_8);

        // Lists what is missing from and what is extra in the generated document.
        assertThat(generated(openApi)).as("endpoints of the generated OpenAPI vs docs/API_SPEC.md")
                .containsExactlyInAnyOrderElementsOf(specified());
    }

    private static Set<String> generated(String openApi) {
        Map<String, Map<String, Object>> paths = JsonPath.read(openApi, "$.paths");
        Set<String> endpoints = new TreeSet<>();
        paths.forEach((path, operations) -> {
            if (!path.startsWith(API_PREFIX)) return;
            operations.keySet().stream()
                    .filter(HTTP_METHODS::contains)
                    .forEach(method -> endpoints.add(endpoint(method, path)));
        });
        return endpoints;
    }

    /**
     * Rows of the endpoint tables: the path is relative to {@code /api/v1}, or to the base in the section title when
     * it does not start with that base (§2 Auth lists {@code /register} under {@code /api/v1/auth}).
     */
    private static Set<String> specified() throws IOException {
        Set<String> endpoints = new TreeSet<>();
        List<String> bases = new ArrayList<>();
        for (String line : Files.readAllLines(API_SPEC, StandardCharsets.UTF_8)) {
            if (SECTION.matcher(line).find()) {
                bases.clear();
                Matcher base = SECTION_BASE.matcher(line);
                while (base.find()) bases.add(base.group(1));
                continue;
            }
            Matcher row = ENDPOINT_ROW.matcher(line);
            if (!row.matches() || row.group(3).contains("❌")) continue;
            String path = API_PREFIX + row.group(2);
            if (!bases.isEmpty() && bases.stream().noneMatch(path::startsWith)) path = bases.getFirst() + row.group(2);
            endpoints.add(endpoint(row.group(1), path));
        }
        return endpoints;
    }

    private static String endpoint(String method, String path) {
        return method.toUpperCase(Locale.ROOT) + " " + path.replaceAll("\\{[^}]+}", "{}");
    }
}
