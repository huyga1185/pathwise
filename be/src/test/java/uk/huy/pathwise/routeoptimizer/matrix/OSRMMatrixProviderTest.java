package uk.huy.pathwise.routeoptimizer.matrix;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OSRMMatrixProviderTest {

    private static final String BASE_URL = "http://osrm.test";

    private MockRestServiceServer mockServer;
    private OSRMMatrixProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new OSRMMatrixProvider(builder.build());
    }

    @Test
    void matrixRoute_buildsLonLatPathAndReturnsDistanceMatrix() {
        // Coordinate(address, lat, lon) -> URL uses "lon,lat"
        Coordinate a = new Coordinate(51.5, -0.1);
        Coordinate b = new Coordinate(52.0, 1.0);

        mockServer.expect(requestTo(
                        BASE_URL + "/table/v1/driving/-0.1,51.5;1.0,52.0?annotations=distance"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"distances\":[[0.0,1234.5],[1234.5,0.0]]}",
                        MediaType.APPLICATION_JSON));

        double[][] result = provider.matrixRoute(List.of(a, b), "driving");

        assertThat(result).isDeepEqualTo(new double[][]{{0.0, 1234.5}, {1234.5, 0.0}});
        mockServer.verify();
    }

    @Test
    void matrixRoute_handlesSingleCoordinate() {
        Coordinate a = new Coordinate(10.0, 20.0);

        mockServer.expect(requestTo(
                        BASE_URL + "/table/v1/car/20.0,10.0?annotations=distance"))
                .andRespond(withSuccess(
                        "{\"distances\":[[0.0]]}",
                        MediaType.APPLICATION_JSON));

        double[][] result = provider.matrixRoute(List.of(a), "car");

        assertThat(result).isDeepEqualTo(new double[][]{{0.0}});
        mockServer.verify();
    }

    @Test
    void matrixRoute_usesGivenProfileInPath() {
        Coordinate a = new Coordinate(1.0, 2.0);
        Coordinate b = new Coordinate(3.0, 4.0);

        mockServer.expect(requestTo(
                        BASE_URL + "/table/v1/bike/2.0,1.0;4.0,3.0?annotations=distance"))
                .andRespond(withSuccess(
                        "{\"distances\":[[0.0,5.0],[5.0,0.0]]}",
                        MediaType.APPLICATION_JSON));

        double[][] result = provider.matrixRoute(List.of(a, b), "bike");

        assertThat(result).isDeepEqualTo(new double[][]{{0.0, 5.0}, {5.0, 0.0}});
    }

    @Test
    void matrixRoute_throwsNullPointer_whenResponseBodyNull() {
        Coordinate a = new Coordinate(1.0, 2.0);

        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.matrixRoute(List.of(a), "driving"))
                .isInstanceOf(NullPointerException.class);
    }
}
