package uk.huy.pathwise.routeoptimizer.routeoptimizationpipeline;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.routeoptimizer.geocoding.GeocodingProvider;
import uk.huy.pathwise.routeoptimizer.matrix.MatrixProvider;
import uk.huy.pathwise.routeoptimizer.solver.SolverProvider;

@Configuration
public class RouteOptimizationPipelineConfig {
    @Bean("nominatimOSRMTools")
    public RouteOptimizationPipeline pipelineNominatimOSRMORTools(
            @Qualifier("NominatimGeocodingProvider") GeocodingProvider geocodingProvider,
            @Qualifier("OSRMMatrixProvider") MatrixProvider matrixProvider,
            @Qualifier("ORToolsSolver")SolverProvider solverProvider) {
        return new RouteOptimizationPipeline(geocodingProvider, matrixProvider, solverProvider);
    }
}
