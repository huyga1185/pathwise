package uk.huy.pathwise.routeoptimizer.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.service.OptimizationService;
import uk.huy.pathwise.core.apiresponse.ApiResponse;

/**
 * Legacy endpoints, removed in T27. Restricted like plan optimization (ARCHITECTURE.md §6.3): OWNER and DISPATCHER.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/route/optimize")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class OptimizationController {
    private final OptimizationService optimizationService;

    @PostMapping
    public ResponseEntity<ApiResponse<OptimizationResponse>> getOptimizedRoute(@RequestBody OptimizationRequest op) {
        OptimizationResponse or = optimizationService.optimizeRoute(op);
        return ResponseEntity.ok().body(new ApiResponse<>(null, or));
    }

    @PostMapping("/k")
    public ResponseEntity<ApiResponse<OptimizationMultiVehicleResponse>> getOptimizedRoute(@RequestBody OptimizationMultiVehicleRequest omvr) {
        OptimizationMultiVehicleResponse omvrs = optimizationService.optimizeRouteForKVehicle(omvr);
        return ResponseEntity.ok().body(new ApiResponse<>(null, omvrs));
    }
}
