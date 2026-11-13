package uk.huy.pathwise.routing.infrastructure.failure;

import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.routing.directions.Directions;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.model.Coordinate;

/**
 * Reports failures of the wrapped directions provider (T54).
 */
public class FailureLoggingDirectionsProvider implements DirectionsProvider {
    private final DirectionsProvider delegate;
    private final String operation;
    private final FailureLog failureLog;

    /**
     * @param providerName e.g. {@code osrm}: the operation is reported as {@code osrm.route}
     */
    public FailureLoggingDirectionsProvider(DirectionsProvider delegate, String providerName, FailureLog failureLog) {
        this.delegate = delegate;
        this.operation = providerName + ".route";
        this.failureLog = failureLog;
    }

    @Override
    public Directions directions(Coordinate from, Coordinate to) {
        return ProviderFailures.observe(failureLog, FailureSource.ROUTING, operation, () -> delegate.directions(from, to));
    }
}
