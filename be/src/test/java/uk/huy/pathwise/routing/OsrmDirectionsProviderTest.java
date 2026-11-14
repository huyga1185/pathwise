package uk.huy.pathwise.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.directions.Directions;
import uk.huy.pathwise.routing.directions.DirectionsStep;
import uk.huy.pathwise.routing.directions.Maneuver;
import uk.huy.pathwise.routing.infrastructure.osrm.OsrmDirectionsProvider;
import uk.huy.pathwise.routing.model.Coordinate;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OsrmDirectionsProviderTest {
    private static final String BASE_URL = "http://osrm.test";
    private static final Coordinate FROM = new Coordinate(16.0544, 108.2022);
    private static final Coordinate TO = new Coordinate(16.0678, 108.2208);
    private static final String URL = BASE_URL
            + "/route/v1/driving/108.2022,16.0544;108.2208,16.0678?overview=full&geometries=polyline6&steps=true";

    /** Shape of a real OSRM v5 answer, trimmed to the fields PathWise reads plus a few it ignores. */
    private static final String ROUTE = """
            {"code":"Ok","waypoints":[{"name":"Lê Duẩn","location":[108.2022,16.0544]}],
             "routes":[{"distance":2412.6,"duration":388.4,"weight":388.4,"geometry":"ozzsl@ssfrnE??",
              "legs":[{"distance":2412.6,"duration":388.4,"summary":"Lê Duẩn, Nguyễn Văn Linh","steps":[
               {"distance":402.3,"duration":61.2,"name":"Lê Duẩn","mode":"driving",
                "maneuver":{"type":"depart","bearing_before":0,"bearing_after":92,"location":[108.2022,16.0544]}},
               {"distance":1210.0,"duration":180.0,"name":"Nguyễn Văn Linh",
                "maneuver":{"type":"turn","modifier":"left","location":[108.2059,16.0546]}},
               {"distance":300.4,"duration":60.5,"name":"",
                "maneuver":{"type":"roundabout","modifier":"right","exit":2,"location":[108.2150,16.0600]}},
               {"distance":499.9,"duration":86.7,"name":"  Bạch Đằng ",
                "maneuver":{"type":"fork","modifier":"slight right","location":[108.2180,16.0640]}},
               {"distance":0,"duration":0,"name":"Bạch Đằng",
                "maneuver":{"type":"arrive","location":[108.2208,16.0678]}}]}]}]}""";

    private MockRestServiceServer mockServer;
    private OsrmDirectionsProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        provider = new OsrmDirectionsProvider(builder.build(), "driving");
    }

    @Test
    void directions_whenOk_mapStepsToOwnVocabularyAndRound() {
        mockServer.expect(requestTo(URL)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(ROUTE, MediaType.APPLICATION_JSON));

        Directions directions = provider.directions(FROM, TO);

        assertThat(directions.distanceMeters()).isEqualTo(2413);
        assertThat(directions.durationSeconds()).isEqualTo(388);
        assertThat(directions.polyline()).isEqualTo("ozzsl@ssfrnE??");
        assertThat(directions.steps()).containsExactly(
                new DirectionsStep(Maneuver.DEPART, null, "Lê Duẩn", 402, 61, new Coordinate(16.0544, 108.2022)),
                new DirectionsStep(Maneuver.LEFT, null, "Nguyễn Văn Linh", 1210, 180, new Coordinate(16.0546, 108.2059)),
                new DirectionsStep(Maneuver.ROUNDABOUT, 2, null, 300, 61, new Coordinate(16.0600, 108.2150)),
                new DirectionsStep(Maneuver.KEEP_RIGHT, null, "Bạch Đằng", 500, 87, new Coordinate(16.0640, 108.2180)),
                new DirectionsStep(Maneuver.ARRIVE, null, null, 0, 0, new Coordinate(16.0678, 108.2208)));
        mockServer.verify();
    }

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
            "depart, null, DEPART",
            "arrive, left, ARRIVE",
            "turn, uturn, UTURN",
            "turn, sharp right, SHARP_RIGHT",
            "turn, right, RIGHT",
            "turn, slight right, SLIGHT_RIGHT",
            "turn, straight, STRAIGHT",
            "turn, slight left, SLIGHT_LEFT",
            "turn, left, LEFT",
            "turn, sharp left, SHARP_LEFT",
            "new name, straight, STRAIGHT",
            "continue, null, STRAIGHT",
            "end of road, right, RIGHT",
            "roundabout turn, left, LEFT",
            "notification, slight left, SLIGHT_LEFT",
            "some future type, right, RIGHT",
            "rotary, null, ROUNDABOUT",
            "exit roundabout, right, ROUNDABOUT",
            "fork, slight left, KEEP_LEFT",
            "merge, sharp right, KEEP_RIGHT",
            "on ramp, right, KEEP_RIGHT",
            "off ramp, straight, STRAIGHT",
            "fork, null, STRAIGHT"})
    void directions_whenOsrmManeuver_mapToPathWiseManeuver(String type, String modifier, Maneuver expected) {
        String modifierJson = modifier == null ? "" : ",\"modifier\":\"" + modifier + "\"";
        mockServer.expect(requestTo(URL)).andRespond(withSuccess("""
                {"code":"Ok","routes":[{"distance":10,"duration":2,"geometry":"??","legs":[{"steps":[
                 {"distance":10,"duration":2,"name":"A","maneuver":{"type":"%s"%s,"location":[108.2,16.05]}}]}]}]}"""
                .formatted(type, modifierJson), MediaType.APPLICATION_JSON));

        assertThat(provider.directions(FROM, TO).steps().getFirst().maneuver()).isEqualTo(expected);
    }

    @Test
    void directions_whenExitOnNonRoundabout_noExitNumber() {
        mockServer.expect(requestTo(URL)).andRespond(withSuccess("""
                {"code":"Ok","routes":[{"distance":10,"duration":2,"geometry":"??","legs":[{"steps":[
                 {"distance":10,"duration":2,"name":"A","maneuver":{"type":"turn","modifier":"left","exit":3,"location":[108.2,16.05]}}]}]}]}""",
                MediaType.APPLICATION_JSON));

        assertThat(provider.directions(FROM, TO).steps().getFirst().exitNumber()).isNull();
    }

    @Test
    void directions_whenNoRoute_routingProviderError() {
        mockServer.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"code\":\"NoRoute\",\"message\":\"Impossible route between points\"}"));

        assertThatThrownBy(() -> provider.directions(FROM, TO))
                .isInstanceOf(ExternalServiceException.class)
                .extracting(e -> ((ExternalServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTING_PROVIDER_ERROR);
    }

    @Test
    void directions_whenCodeNotOk_routingProviderError() {
        mockServer.expect(requestTo(URL)).andRespond(withSuccess("{\"code\":\"NoSegment\",\"routes\":[]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.directions(FROM, TO)).isInstanceOf(ExternalServiceException.class);
    }

    @Test
    void directions_whenTimeout_routingProviderError() {
        mockServer.expect(requestTo(URL)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> provider.directions(FROM, TO))
                .isInstanceOf(ExternalServiceException.class)
                .extracting(e -> ((ExternalServiceException) e).getErrorCode())
                .isEqualTo(ErrorCode.ROUTING_PROVIDER_ERROR);
    }

    @Test
    void directions_whenIncompleteRoute_routingProviderError() {
        String[] bodies = {
                "{\"code\":\"Ok\",\"routes\":[]}",
                "{\"code\":\"Ok\",\"routes\":[{\"distance\":1,\"duration\":1,\"geometry\":\"\",\"legs\":[{\"steps\":[]}]}]}",
                "{\"code\":\"Ok\",\"routes\":[{\"distance\":1,\"duration\":1,\"geometry\":\"??\",\"legs\":[{\"steps\":[]}]}]}",
                "{\"code\":\"Ok\",\"routes\":[{\"distance\":1,\"duration\":1,\"geometry\":\"??\",\"legs\":[]}]}",
                "{\"code\":\"Ok\",\"routes\":[{\"distance\":null,\"duration\":1,\"geometry\":\"??\",\"legs\":[{\"steps\":["
                        + "{\"distance\":1,\"duration\":1,\"maneuver\":{\"type\":\"depart\",\"location\":[108.2,16.05]}}]}]}]}",
                "{\"code\":\"Ok\",\"routes\":[{\"distance\":1,\"duration\":1,\"geometry\":\"??\",\"legs\":[{\"steps\":["
                        + "{\"distance\":1,\"duration\":1,\"maneuver\":{\"type\":\"depart\"}}]}]}]}"};
        for (String body : bodies) {
            setUp();
            mockServer.expect(requestTo(URL)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> provider.directions(FROM, TO))
                    .as(body)
                    .isInstanceOf(ExternalServiceException.class);
        }
    }
}
