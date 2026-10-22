package uk.huy.pathwise.routeoptimizer.geocoding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.routeoptimizer.infrastructure.geocoding.nominatimprovider.NominatimGeocodingProvider;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodingProviderTest {

    private static final String BASE_URL = "http://nominatim.test";

    private MockRestServiceServer mockServer;
    private NominatimGeocodingProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new NominatimGeocodingProvider(builder.build());
    }

    @Test
    void geocode_returnsCoordinate_whenAddressFound() throws AppException {
        mockServer.expect(requestTo(BASE_URL + "/search?q=Big%20Ben,%20London&format=json&limit=1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"lat\":\"51.5007292\",\"lon\":\"-0.1268141\"}]",
                        MediaType.APPLICATION_JSON));

        Coordinate result = provider.geocode("Big Ben, London");

        assertThat(result.lat()).isEqualTo(51.5007292);
        assertThat(result.lon()).isEqualTo(-0.1268141);
        mockServer.verify();
    }

    @Test
    void geocode_usesFirstResult_whenMultipleReturned() throws AppException {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"lat\":\"1.0\",\"lon\":\"2.0\"},{\"lat\":\"3.0\",\"lon\":\"4.0\"}]",
                        MediaType.APPLICATION_JSON));

        Coordinate result = provider.geocode("somewhere");

        assertThat(result.lat()).isEqualTo(1.0);
        assertThat(result.lon()).isEqualTo(2.0);
    }

    @Test
    void geocode_throwsCouldNotFindAddress_whenResultEmpty() {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.geocode("nowhere"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COULD_NOT_FIND_ADDRESS);
    }

    @Test
    void geocode_throwsCouldNotFindAddress_whenResponseBodyNull() {
        mockServer.expect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.geocode("nowhere"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COULD_NOT_FIND_ADDRESS);
    }

    @Test
    void geocodeListAddress_returnsCoordinateForEachAddress() throws AppException {
        mockServer.expect(requestTo(BASE_URL + "/search?q=A&format=json&limit=1"))
                .andRespond(withSuccess("[{\"lat\":\"1.0\",\"lon\":\"2.0\"}]", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/search?q=B&format=json&limit=1"))
                .andRespond(withSuccess("[{\"lat\":\"3.0\",\"lon\":\"4.0\"}]", MediaType.APPLICATION_JSON));

        List<Coordinate> results = provider.geocodeListAddress(List.of("A", "B"));

        assertThat(results).hasSize(2);
        assertThat(results.get(0).lat()).isEqualTo(1.0);
        assertThat(results.get(1).lon()).isEqualTo(4.0);
        mockServer.verify();
    }

    @Test
    void geocodeListAddress_returnsEmptyList_whenNoAddresses() throws AppException {
        List<Coordinate> results = provider.geocodeListAddress(List.of());

        assertThat(results).isEmpty();
        mockServer.verify();
    }

    @Test
    void geocodeListAddress_propagatesException_whenAnAddressNotFound() {
        mockServer.expect(requestTo(BASE_URL + "/search?q=A&format=json&limit=1"))
                .andRespond(withSuccess("[{\"lat\":\"1.0\",\"lon\":\"2.0\"}]", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo(BASE_URL + "/search?q=B&format=json&limit=1"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.geocodeListAddress(List.of("A", "B")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COULD_NOT_FIND_ADDRESS);
    }
}
