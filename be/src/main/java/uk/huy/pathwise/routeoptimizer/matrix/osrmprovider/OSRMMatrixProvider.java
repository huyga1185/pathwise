package uk.huy.pathwise.routeoptimizer.matrix.osrmprovider;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.routeoptimizer.matrix.MatrixProvider;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Qualifier("OSRMMatrixProvider")
public class OSRMMatrixProvider implements MatrixProvider {
    private final RestClient osrmClient;

    public OSRMMatrixProvider(@Qualifier("osrmRestClient") final RestClient osrmClient) {
        this.osrmClient = osrmClient;
    }

    @Override
    public double[][] matrixRoute(final List<Coordinate> coordinates, final String profile) {
        String coords = coordinates.stream()
                .map(c -> c.lon() + "," + c.lat())
                .collect(Collectors.joining(";"));

        OSRMResult result = osrmClient.get()
                .uri(uriBuilder -> uriBuilder
                        .pathSegment("table", "v1", profile, coords)
                        .queryParam("annotations", "distance")
                        .build())
                .retrieve()
                .body(OSRMResult.class);
        return result.distances();
    }
}
