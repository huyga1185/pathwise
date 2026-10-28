package uk.huy.pathwise.planning.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteDetailResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteSummaryResponse;
import uk.huy.pathwise.planning.service.DriverRouteService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.time.LocalDate;
import java.util.List;

/**
 * Driver API (API_SPEC §8): a DRIVER, or an OWNER/DISPATCHER with {@code canDrive} (D-21, ARCHITECTURE.md §6.3).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driver/routes")
@PreAuthorize("@authz.canDrive(principal)")
public class DriverRouteController {
    private final DriverRouteService driverRouteService;

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
}
