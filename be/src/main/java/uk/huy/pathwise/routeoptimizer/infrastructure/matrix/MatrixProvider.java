package uk.huy.pathwise.routeoptimizer.infrastructure.matrix;

import uk.huy.pathwise.routeoptimizer.model.Coordinate;

import java.util.List;

public interface MatrixProvider {
    double[][] matrixRoute(final List<Coordinate> coordinates, final String profile);
}
