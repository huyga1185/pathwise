package uk.huy.pathwise.routing.matrix;

import uk.huy.pathwise.routing.model.Coordinate;

import java.util.List;

/**
 * Public API of the {@code routing} module for travel matrices (ARCHITECTURE.md §8.3, §8.5). The bean is the provider
 * chosen by {@code routing.matrix.provider}.
 */
public interface MatrixProvider {
    /**
     * @param points at least one point; the matrix uses the same order
     * @throws uk.huy.pathwise.core.exception.ExternalServiceException {@code ROUTING_PROVIDER_ERROR} if the provider
     *         fails or times out, or returns no value for a pair of points (no road between them)
     */
    TravelMatrix matrix(List<Coordinate> points);
}
