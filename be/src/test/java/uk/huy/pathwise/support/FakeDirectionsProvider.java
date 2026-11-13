package uk.huy.pathwise.support;

import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.directions.Directions;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.directions.DirectionsStep;
import uk.huy.pathwise.routing.directions.Maneuver;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Directions without OSRM: a straight line from one point to the other, depart then arrive. Records the last call,
 * and whether it ran inside a transaction (external calls must not, ARCHITECTURE.md §8.5).
 */
public class FakeDirectionsProvider implements DirectionsProvider {
    /** Any encoded polyline: the server passes it through unchanged. */
    public static final String POLYLINE = "_izlhA~rlgdF_{geC~ywl@";

    private final AtomicReference<Coordinate[]> lastCall = new AtomicReference<>();
    private final AtomicBoolean lastCallInTransaction = new AtomicBoolean();
    private final AtomicBoolean failNext = new AtomicBoolean();

    @Override
    public Directions directions(Coordinate from, Coordinate to) {
        lastCall.set(new Coordinate[]{from, to});
        lastCallInTransaction.set(TransactionSynchronizationManager.isActualTransactionActive());
        if (failNext.getAndSet(false))
            throw new ExternalServiceException(ErrorCode.ROUTING_PROVIDER_ERROR, "Simulated OSRM failure", null);
        return new Directions(1500, 180, POLYLINE, List.of(
                new DirectionsStep(Maneuver.DEPART, null, "Lê Duẩn", 1500, 180, from),
                new DirectionsStep(Maneuver.ARRIVE, null, null, 0, 0, to)));
    }

    /**
     * The next call fails like an unreachable OSRM.
     */
    public void failNext() {
        failNext.set(true);
    }

    /**
     * @return {from, to} of the last call, or null if none
     */
    public Coordinate[] lastCall() {
        return lastCall.get();
    }

    public boolean lastCallInTransaction() {
        return lastCallInTransaction.get();
    }

    public void reset() {
        lastCall.set(null);
        lastCallInTransaction.set(false);
        failNext.set(false);
    }
}
