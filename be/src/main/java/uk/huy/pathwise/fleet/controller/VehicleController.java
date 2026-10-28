package uk.huy.pathwise.fleet.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.fleet.dto.request.CreateVehicleRequest;
import uk.huy.pathwise.fleet.dto.request.UpdateVehicleRequest;
import uk.huy.pathwise.fleet.dto.response.VehicleResponse;
import uk.huy.pathwise.fleet.service.VehicleService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.util.List;

/**
 * Vehicles of the caller's store (API_SPEC §5). OWNER and DISPATCHER only (ARCHITECTURE.md §6.3). There is no delete:
 * a vehicle is deactivated with {@code active = false}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vehicles")
@PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
public class VehicleController {
    private final VehicleService vehicleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> listVehicles(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                           @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(new ApiResponse<>(vehicleService.listVehicles(actor.identity(), active)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                      @Valid @RequestBody CreateVehicleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(vehicleService.createVehicle(actor.identity(), request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicle(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                   @PathVariable("id") long vehicleId) {
        return ResponseEntity.ok(new ApiResponse<>(vehicleService.getVehicle(actor.identity(), vehicleId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                      @PathVariable("id") long vehicleId,
                                                                      @Valid @RequestBody UpdateVehicleRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(vehicleService.updateVehicle(actor.identity(), vehicleId, request)));
    }
}
