package uk.huy.pathwise.support;

import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Travel matrix computed from the coordinates, so that no test calls OSRM: straight-line distance, driven at 10 m/s.
 * A test can make the next call fail, run code during it (for example change the plan), or block it.
 */
public class FakeMatrixProvider implements MatrixProvider {
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final double SPEED_METERS_PER_SECOND = 10;

    private final AtomicReference<Runnable> nextCall = new AtomicReference<>();

    @Override
    public TravelMatrix matrix(List<Coordinate> points) {
        Runnable action = nextCall.getAndSet(null);
        if (action != null) action.run();
        int n = points.size();
        int[][] distance = new int[n][n];
        int[][] duration = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                distance[i][j] = (int) Math.round(meters(points.get(i), points.get(j)));
                duration[i][j] = (int) Math.round(distance[i][j] / SPEED_METERS_PER_SECOND);
            }
        }
        return new TravelMatrix(distance, duration);
    }

    /**
     * The next call fails like an unreachable OSRM.
     */
    public void failNext() {
        nextCall.set(() -> {
            throw new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR, "Simulated OSRM failure", null);
        });
    }

    /**
     * Runs {@code action} at the start of the next call, before the matrix is returned.
     */
    public void onNextCall(Runnable action) {
        nextCall.set(action);
    }

    /**
     * The next call blocks until the returned latch is counted down, to keep a job RUNNING.
     */
    public CountDownLatch holdNext() {
        CountDownLatch latch = new CountDownLatch(1);
        nextCall.set(() -> {
            try {
                if (!latch.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Held matrix never released");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        });
        return latch;
    }

    private static double meters(Coordinate a, Coordinate b) {
        double lat1 = Math.toRadians(a.lat());
        double lat2 = Math.toRadians(b.lat());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.lon() - a.lon());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(h));
    }
}
