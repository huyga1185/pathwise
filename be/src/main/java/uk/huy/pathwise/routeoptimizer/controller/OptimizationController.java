package uk.huy.pathwise.routeoptimizer.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.service.OptimizationService;
import uk.huy.pathwise.shared.response.ApiResponse;

@RestController
@AllArgsConstructor
@RequestMapping("/route/optimize")
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
