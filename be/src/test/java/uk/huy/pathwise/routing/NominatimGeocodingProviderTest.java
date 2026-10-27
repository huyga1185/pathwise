package uk.huy.pathwise.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;
import uk.huy.pathwise.routing.infrastructure.nominatim.NominatimGeocodingProvider;

import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NominatimGeocodingProviderTest {
    private static final String BASE_URL = "http://nominatim.test";

    private MockRestServiceServer mockServer;
    private NominatimGeocodingProvider provider;
    private final AtomicBoolean slotFree = new AtomicBoolean(true);

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new NominatimGeocodingProvider(builder.build(), slotFree::get);
    }

    @Test
    void search_whenFound_sendVietnamOnlyQueryAndParseResults() {
        mockServer.expect(method(HttpMethod.GET))
                .andExpect(requestTo(org.hamcrest.Matchers.startsWith(BASE_URL + "/search?")))
                .andExpect(queryParam("q", "12%20L%C3%AA%20Du%E1%BA%A9n,%20%C4%90%C3%A0%20N%E1%BA%B5ng"))
                .andExpect(queryParam("format", "json"))
                .andExpect(queryParam("countrycodes", "vn"))
                .andExpect(queryParam("limit", "3"))
                .andRespond(withSuccess("""
                        [{"display_name":"12 Lê Duẩn, Hải Châu, Đà Nẵng","lat":"16.0718","lon":"108.2208"},
                         {"display_name":"Lê Duẩn, Thanh Khê","lat":"16.0660","lon":"108.2100"}]""", MediaType.APPLICATION_JSON));

        assertThat(provider.search("12 Lê Duẩn, Đà Nẵng", 3)).containsExactly(
                new GeocodingResult("12 Lê Duẩn, Hải Châu, Đà Nẵng", 16.0718, 108.2208),
                new GeocodingResult("Lê Duẩn, Thanh Khê", 16.0660, 108.2100));
        mockServer.verify();
    }

    @Test
    void search_whenNothingFound_returnEmptyList() {
        mockServer.expect(method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(provider.search("nowhere", 1)).isEmpty();
    }

    @Test
    void search_whenProviderReturns5xx_throwGeocodingProviderError() {
        mockServer.expect(method(HttpMethod.GET)).andRespond(withServerError());

        assertThatThrownBy(() -> provider.search("somewhere", 1))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_PROVIDER_ERROR);
    }

    @Test
    void search_whenProviderTimesOut_throwGeocodingProviderError() {
        mockServer.expect(method(HttpMethod.GET)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> provider.search("somewhere", 1))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_PROVIDER_ERROR);
    }

    @Test
    void search_whenCoordinatesInvalid_throwGeocodingProviderError() {
        mockServer.expect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"display_name\":\"x\",\"lat\":\"abc\",\"lon\":\"1\"}]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.search("somewhere", 1))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_PROVIDER_ERROR);
    }

    @Test
    void search_whenNoRateLimitSlot_throwGeocodingBusyWithoutCallingProvider() {
        slotFree.set(false);
        mockServer.expect(never(), method(HttpMethod.GET));

        assertThatThrownBy(() -> provider.search("somewhere", 1))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_BUSY);
        mockServer.verify();
    }
}
