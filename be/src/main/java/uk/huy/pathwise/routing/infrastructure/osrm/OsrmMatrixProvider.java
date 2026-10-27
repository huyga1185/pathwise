package uk.huy.pathwise.routing.infrastructure.osrm;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OSRM {@code /table} with {@code annotations=distance,duration}. Values are rounded to whole metres and seconds.
 * Any response that is not a complete matrix of numbers is a provider error: a missing or null cell means OSRM found
 * no road between two points, and the solver cannot work with a hole in the matrix.
 * <p>
 * An OSRM server refuses tables larger than its {@code --max-table-size} (100 on the public server and by default),
 * which a valid plan can exceed (100 stops plus the route ends, D-19, F-41). Up to {@code maxTableSize} points go in
 * one request. Above that the points are split into blocks of {@code maxTableSize / 2} and the matrix is filled block
 * pair by block pair, with {@code sources} and {@code destinations} selecting the block pair: each request then has at
 * most {@code maxTableSize} coordinates, so it stays within the limit whether the server counts coordinates or
 * sources × destinations. The cost is more round trips for large plans (about {@code (n / (maxTableSize / 2))²}).
 */
public class OsrmMatrixProvider implements MatrixProvider {
    private final RestClient restClient;
    private final String profile;
    private final int maxTableSize;

    /**
     * @param maxTableSize the {@code --max-table-size} of the OSRM server, at least 2
     */
    public OsrmMatrixProvider(RestClient restClient, String profile, int maxTableSize) {
        if (maxTableSize < 2) throw new IllegalArgumentException("maxTableSize must be at least 2");
        this.restClient = restClient;
        this.profile = profile;
        this.maxTableSize = maxTableSize;
    }

    @Override
    public TravelMatrix matrix(List<Coordinate> points) {
        if (points == null || points.isEmpty()) throw new IllegalArgumentException("At least one point is required");
        int n = points.size();
        if (n <= maxTableSize) {
            Block all = new Block(IntStream.range(0, n).boxed().toList());
            OsrmTableResult result = request(points, null, null);
            int[][] distances = new int[n][n];
            int[][] durations = new int[n][n];
            copy(result.distances(), all, all, distances, "distances");
            copy(result.durations(), all, all, durations, "durations");
            return new TravelMatrix(distances, durations);
        }

        List<Block> blocks = blocks(n, maxTableSize / 2);
        int[][] distances = new int[n][n];
        int[][] durations = new int[n][n];
        for (Block from : blocks) {
            for (Block to : blocks) {
                // Coordinates of this request: the source block, then the destination block if it is another one.
                List<Integer> indexes = new ArrayList<>(from.indexes());
                if (to != from) indexes.addAll(to.indexes());
                List<Integer> sources = IntStream.range(0, from.size()).boxed().toList();
                List<Integer> destinations = to == from
                        ? sources
                        : IntStream.range(from.size(), from.size() + to.size()).boxed().toList();
                OsrmTableResult result = request(indexes.stream().map(points::get).toList(), sources, destinations);
                copy(result.distances(), from, to, distances, "distances");
                copy(result.durations(), from, to, durations, "durations");
            }
        }
        return new TravelMatrix(distances, durations);
    }

    private record Block(List<Integer> indexes) {
        int size() {
            return indexes.size();
        }
    }

    private static List<Block> blocks(int n, int blockSize) {
        List<Block> blocks = new ArrayList<>();
        for (int start = 0; start < n; start += blockSize)
            blocks.add(new Block(IntStream.range(start, Math.min(n, start + blockSize)).boxed().toList()));
        return blocks;
    }

    private OsrmTableResult request(List<Coordinate> points, List<Integer> sources, List<Integer> destinations) {
        String coordinates = points.stream()
                .map(point -> point.lon() + "," + point.lat())
                .collect(Collectors.joining(";"));
        OsrmTableResult result;
        try {
            result = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.pathSegment("table", "v1", profile, coordinates)
                                .queryParam("annotations", "distance,duration");
                        if (sources != null) uriBuilder.queryParam("sources", join(sources));
                        if (destinations != null) uriBuilder.queryParam("destinations", join(destinations));
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(OsrmTableResult.class);
        } catch (RestClientException e) {
            // HTTP error status (OSRM answers 400 with a code such as "TooBig"), timeout, connection or parse failure.
            throw error("OSRM table request failed", e);
        }
        if (result == null || !"Ok".equals(result.code()))
            throw error("OSRM table returned code " + (result == null ? null : result.code()), null);
        return result;
    }

    /**
     * Copies a {@code from.size() × to.size()} block of OSRM values, rounded, into the full matrix.
     */
    private static void copy(Double[][] values, Block from, Block to, int[][] target, String field) {
        if (values == null || values.length != from.size()) throw error("OSRM table has no complete " + field, null);
        for (int i = 0; i < from.size(); i++) {
            if (values[i] == null || values[i].length != to.size()) throw error("OSRM table has no complete " + field, null);
            for (int j = 0; j < to.size(); j++) {
                Double value = values[i][j];
                int source = from.indexes().get(i);
                int destination = to.indexes().get(j);
                if (value == null) throw error("OSRM found no route from point " + source + " to point " + destination, null);
                if (value < 0 || value > Integer.MAX_VALUE) throw error("OSRM returned an invalid " + field + " value", null);
                target[source][destination] = (int) Math.round(value);
            }
        }
    }

    private static String join(List<Integer> indexes) {
        return indexes.stream().map(String::valueOf).collect(Collectors.joining(";"));
    }

    private static ExternalServiceException error(String message, Throwable cause) {
        return new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR, message, cause);
    }
}
