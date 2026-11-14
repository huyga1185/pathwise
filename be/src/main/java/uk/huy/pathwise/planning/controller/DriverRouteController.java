package uk.huy.pathwise.planning.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.web.AllowedWhenStoreReadOnly;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.planning.dto.request.UpdateStopStatusRequest;
import uk.huy.pathwise.planning.dto.response.DirectionsResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteDetailResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteSummaryResponse;
import uk.huy.pathwise.planning.dto.response.DriverStopUpdateResponse;
import uk.huy.pathwise.planning.service.DriverDirectionsService;
import uk.huy.pathwise.planning.service.DriverRouteService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.time.LocalDate;
import java.util.List;

/**
 * Driver API (API_SPEC §8): a DRIVER, or an OWNER/DISPATCHER with {@code canDrive} (D-21, ARCHITECTURE.md §6.3).
 */
// Drivers keep reporting stop results in a locked store, to finish running deliveries (T56, P-59).
@AllowedWhenStoreReadOnly
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driver/routes")
@PreAuthorize("@authz.canDrive(principal)")
public class DriverRouteController {
    private final DriverRouteService driverRouteService;
    private final DriverDirectionsService driverDirectionsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverRouteSummaryResponse>>> listRoutes(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(new ApiResponse<>(driverRouteService.listRoutes(actor.identity(), date)));
    }

    @GetMapping("/{routeId}")
    public ResponseEntity<ApiResponse<DriverRouteDetailResponse>> getRoute(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                           @PathVariable("routeId") long routeId) {
        return ResponseEntity.ok(new ApiResponse<>(driverRouteService.getRoute(actor.identity(), routeId)));
    }

    /**
     * Directions from the driver's current position (T33, P-57). A missing or non-numeric {@code lat}/{@code lon} is a
     * malformed request; one out of range is a validation error.
     */
    @GetMapping("/{routeId}/directions")
    public ResponseEntity<ApiResponse<DirectionsResponse>> getDirections(
            @AuthenticationPrincipal AuthenticatedUser actor,
            @PathVariable("routeId") long routeId,
            @RequestParam("lat") @DecimalMin(value = "-90", message = "INVALID_LATITUDE") @DecimalMax(value = "90", message = "INVALID_LATITUDE") double lat,
            @RequestParam("lon") @DecimalMin(value = "-180", message = "INVALID_LONGITUDE") @DecimalMax(value = "180", message = "INVALID_LONGITUDE") double lon,
            @RequestParam(value = "toStopId", required = false) Long toStopId) {
        return ResponseEntity.ok(new ApiResponse<>(
                driverDirectionsService.directions(actor.identity(), routeId, lat, lon, toStopId)));
    }

    @PatchMapping("/{routeId}/stops/{stopId}/status")
    public ResponseEntity<ApiResponse<DriverStopUpdateResponse>> updateStopStatus(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                                  @PathVariable long routeId,
                                                                                  @PathVariable long stopId,
                                                                                @Valid @RequestBody UpdateStopStatusRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(driverRouteService.updateStopStatus(actor.identity(), routeId, stopId, request)));
    }
}
