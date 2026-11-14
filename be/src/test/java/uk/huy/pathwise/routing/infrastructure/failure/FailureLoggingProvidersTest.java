package uk.huy.pathwise.routing.infrastructure.failure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FailureLoggingProvidersTest {
    private static final List<Coordinate> POINTS = List.of(new Coordinate(16.05, 108.2), new Coordinate(16.06, 108.21));
    private final FailureLog failureLog = mock(FailureLog.class);

    @Test
    void matrix_whenServerUnreachable_reportUnreachableAndRethrow() {
        MatrixProvider osrm = mock(MatrixProvider.class);
        ExternalServiceException down = new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR,
                "OSRM table request failed", new ResourceAccessException("Connection refused"));
        when(osrm.matrix(POINTS)).thenThrow(down);
        MatrixProvider provider = new FailureLoggingMatrixProvider(osrm, "osrm", failureLog);

        assertThatThrownBy(() -> provider.matrix(POINTS)).isSameAs(down);

        verify(failureLog).record(FailureSource.ROUTING, "osrm.table", "unreachable",
                "message=\"OSRM table request failed\"", null);
    }

    @Test
    void matrix_whenServerAnswers_returnItsResultAndReportNothing() {
        MatrixProvider osrm = mock(MatrixProvider.class);
        TravelMatrix matrix = new TravelMatrix(new int[][]{{0, 1}, {1, 0}}, new int[][]{{0, 1}, {1, 0}});
        when(osrm.matrix(POINTS)).thenReturn(matrix);

        assertThat(new FailureLoggingMatrixProvider(osrm, "osrm", failureLog).matrix(POINTS)).isSameAs(matrix);

        verifyNoInteractions(failureLog);
    }

    @Test
    void directions_whenServerAnswersHttpError_reportItsStatus() {
        DirectionsProvider osrm = mock(DirectionsProvider.class);
        when(osrm.directions(any(), any())).thenThrow(new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR,
                "OSRM route request failed", HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", null, null, null)));
        DirectionsProvider provider = new FailureLoggingDirectionsProvider(osrm, "osrm", failureLog);

        assertThatThrownBy(() -> provider.directions(POINTS.get(0), POINTS.get(1))).isInstanceOf(ExternalServiceException.class);

        verify(failureLog).record(eq(FailureSource.ROUTING), eq("osrm.route"), eq("http_400"), anyString(), isNull());
    }

    @Test
    void search_whenInvalidReply_reportBadResponseWithoutTheQuery() {
        GeocodingProvider nominatim = mock(GeocodingProvider.class);
        when(nominatim.search(anyString(), anyInt())).thenThrow(new ExternalServiceException(
                ErrorCode.GEOCODING_PROVIDER_ERROR, "Nominatim returned invalid coordinates", null));
        GeocodingProvider provider = new FailureLoggingGeocodingProvider(nominatim, "nominatim", failureLog);

        assertThatThrownBy(() -> provider.search("12 lê duẩn", 5)).isInstanceOf(ExternalServiceException.class);

        verify(failureLog).record(FailureSource.GEOCODING, "nominatim.search", "bad_response",
                "message=\"Nominatim returned invalid coordinates\"", null);
    }

    @Test
    void search_whenOwnRateLimitBusy_reportNothing() {
        GeocodingProvider nominatim = mock(GeocodingProvider.class);
        when(nominatim.search(anyString(), anyInt())).thenThrow(new ExternalServiceException(
                ErrorCode.GEOCODING_BUSY, "No Nominatim rate limit slot in time", null));
        GeocodingProvider provider = new FailureLoggingGeocodingProvider(nominatim, "nominatim", failureLog);

        assertThatThrownBy(() -> provider.search("12 lê duẩn", 5)).isInstanceOf(ExternalServiceException.class);

        verify(failureLog, never()).record(any(), any(), any(), any(), any());
    }
}
