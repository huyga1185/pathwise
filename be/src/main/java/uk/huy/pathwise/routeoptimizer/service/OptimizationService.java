package uk.huy.pathwise.routeoptimizer.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.infrastructure.routeoptimizationpipeline.RouteOptimizationPipeline;

import java.util.List;

@Service
public class OptimizationService {
    private final RouteOptimizationPipeline routeOptimizationPipeline;

    public OptimizationService(@Qualifier("nominatimOSRMTools") RouteOptimizationPipeline routeOptimizationPipeline) {
        this.routeOptimizationPipeline = routeOptimizationPipeline;
    }

    public OptimizationResponse optimizeRoute(OptimizationRequest op) {
        List<String> optimizedRoute = routeOptimizationPipeline.optimize(op.getAddresses(), op.getDepot());
        return OptimizationResponse.builder()
                .optimizedRoute(optimizedRoute)
                .depot(op.getDepot()).build();
    }

    public OptimizationMultiVehicleResponse optimizeRouteForKVehicle(OptimizationMultiVehicleRequest omvr) {
        List<List<String>> optimizedRoute = routeOptimizationPipeline.optimizeKVehicleRoute(omvr.addresses(), omvr.depot(), omvr.numberVehicle());
        return new OptimizationMultiVehicleResponse(optimizedRoute, omvr.depot());
    }
}
