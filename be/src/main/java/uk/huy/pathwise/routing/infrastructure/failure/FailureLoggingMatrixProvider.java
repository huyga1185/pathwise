package uk.huy.pathwise.routing.infrastructure.failure;

import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;

import java.util.List;

/**
 * Reports failures of the wrapped matrix provider (T54).
 */
public class FailureLoggingMatrixProvider implements MatrixProvider {
    private final MatrixProvider delegate;
    private final String operation;
    private final FailureLog failureLog;

    /**
     * @param providerName e.g. {@code osrm}: the operation is reported as {@code osrm.table}
     */
    public FailureLoggingMatrixProvider(MatrixProvider delegate, String providerName, FailureLog failureLog) {
        this.delegate = delegate;
        this.operation = providerName + ".table";
        this.failureLog = failureLog;
    }

    @Override
    public TravelMatrix matrix(List<Coordinate> points) {
        return ProviderFailures.observe(failureLog, FailureSource.ROUTING, operation, () -> delegate.matrix(points));
    }
}
