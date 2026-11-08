package uk.huy.pathwise.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.infrastructure.osrm.OsrmMatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsrmMatrixProviderTest {
    private static final String BASE_URL = "http://osrm.test";
    private static final List<Coordinate> TWO_POINTS = List.of(new Coordinate(16.05, 108.2), new Coordinate(16.07, 108.22));

    private MockRestServiceServer mockServer;
    private OsrmMatrixProvider provider;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new OsrmMatrixProvider(builder.build(), "driving", 100);
        this.builder = builder;
    }

    @Test
    void matrix_whenOk_requestDistanceAndDurationAndRoundToWholeUnits() {
        mockServer.expect(requestTo(BASE_URL + "/table/v1/driving/108.2,16.05;108.22,16.07?annotations=distance,duration"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"code":"Ok",
                         "distances":[[0,3012.4],[2987.6,0]],
                         "durations":[[0,420.5],[398.2,0]]}""", MediaType.APPLICATION_JSON));

        TravelMatrix matrix = provider.matrix(TWO_POINTS);

        assertThat(matrix.size()).isEqualTo(2);
        assertThat(matrix.distanceMeters()).isDeepEqualTo(new int[][]{{0, 3012}, {2988, 0}});
        assertThat(matrix.durationSeconds()).isDeepEqualTo(new int[][]{{0, 421}, {398, 0}});
        mockServer.verify();
    }

    @Test
    void matrix_whenPairHasNoRoute_throwRoutingProviderError() {
        respond("""
                {"code":"Ok","distances":[[0,null],[2987.6,0]],"durations":[[0,null],[398.2,0]]}""");

        assertProviderError();
    }

    @Test
    void matrix_whenDurationsMissing_throwRoutingProviderError() {
        respond("""
                {"code":"Ok","distances":[[0,3012.4],[2987.6,0]]}""");

        assertProviderError();
    }

    @Test
    void matrix_whenRowShorterThanPointCount_throwRoutingProviderError() {
        respond("""
                {"code":"Ok","distances":[[0],[2987.6,0]],"durations":[[0,420.5],[398.2,0]]}""");

        assertProviderError();
    }

    @Test
    void matrix_whenCodeNotOk_throwRoutingProviderError() {
        respond("""
                {"code":"NoTable"}""");

        assertProviderError();
    }

    @Test
    void matrix_whenOsrmAnswers400_throwRoutingProviderError() {
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":\"TooBig\",\"message\":\"Too many table coordinates\"}"));

        assertProviderError();
    }

    @Test
    void matrix_whenTimeout_throwRoutingProviderError() {
        mockServer.expect(method(HttpMethod.GET)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertProviderError();
    }

    @Test
    void matrix_whenMorePointsThanMaxTableSize_fillMatrixFromBlockRequestsWithinLimit() {
        // max-table-size 4 → blocks of 2 points: [0,1] [2,3] [4], 3 × 3 block pairs = 9 requests.
        OsrmMatrixProvider small = new OsrmMatrixProvider(builder.build(), "driving", 4);
        List<Coordinate> points = IntStream.range(0, 5).mapToObj(i -> new Coordinate(16.0, i + 1)).toList();
        List<Integer> coordinatesPerRequest = new ArrayList<>();
        mockServer.expect(ExpectedCount.times(9), method(HttpMethod.GET))
                .andRespond(request -> fakeOsrm(request.getURI(), coordinatesPerRequest));

        TravelMatrix matrix = small.matrix(points);

        for (int i = 0; i < 5; i++)
            for (int j = 0; j < 5; j++) {
                assertThat(matrix.distanceMeters()[i][j]).as("distance %d→%d", i, j).isEqualTo(100 * i + j);
                assertThat(matrix.durationSeconds()[i][j]).as("duration %d→%d", i, j).isEqualTo(2 * (100 * i + j));
            }
        assertThat(coordinatesPerRequest).hasSize(9).allMatch(count -> count <= 4);
        mockServer.verify();
    }

    @Test
    void matrix_whenBlockRequestHasNullCell_throwRoutingProviderError() {
        OsrmMatrixProvider small = new OsrmMatrixProvider(builder.build(), "driving", 2);
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":\"Ok\",\"distances\":[[0]],\"durations\":[[0]]}", MediaType.APPLICATION_JSON));
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":\"Ok\",\"distances\":[[null]],\"durations\":[[5]]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> small.matrix(List.of(new Coordinate(16, 1), new Coordinate(16, 2), new Coordinate(16, 3))))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROUTING_PROVIDER_ERROR);
    }

    /**
     * Answers like OSRM for points whose longitude is {@code index + 1}: value {@code 100 × from + to} (distance) and
     * twice that (duration), restricted to the requested sources and destinations.
     */
    private static ClientHttpResponse fakeOsrm(URI uri, List<Integer> coordinatesPerRequest) throws IOException {
        String path = uri.getPath();
        String[] coordinates = path.substring(path.lastIndexOf('/') + 1).split(";");
        coordinatesPerRequest.add(coordinates.length);
        int[] index = Arrays.stream(coordinates).mapToInt(c -> (int) Double.parseDouble(c.split(",")[0]) - 1).toArray();
        Map<String, String> query = Arrays.stream(uri.getQuery().split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(param -> param[0], param -> param[1]));
        int[] sources = positions(query.get("sources"), index.length);
        int[] destinations = positions(query.get("destinations"), index.length);
        String distances = Arrays.stream(sources)
                .mapToObj(s -> Arrays.stream(destinations).mapToObj(d -> String.valueOf(100 * index[s] + index[d]))
                        .collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(",", "[", "]"));
        String durations = Arrays.stream(sources)
                .mapToObj(s -> Arrays.stream(destinations).mapToObj(d -> String.valueOf(2 * (100 * index[s] + index[d])))
                        .collect(Collectors.joining(",", "[", "]")))
                .collect(Collectors.joining(",", "[", "]"));
        return withSuccess("{\"code\":\"Ok\",\"distances\":" + distances + ",\"durations\":" + durations + "}",
                MediaType.APPLICATION_JSON).createResponse(null);
    }

    private static int[] positions(String param, int count) {
        if (param == null) return IntStream.range(0, count).toArray();
        return Arrays.stream(param.split(";")).mapToInt(Integer::parseInt).toArray();
    }

    private void respond(String body) {
        mockServer.expect(method(HttpMethod.GET)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void assertProviderError() {
        assertThatThrownBy(() -> provider.matrix(TWO_POINTS))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.ROUTING_PROVIDER_ERROR);
    }
}
