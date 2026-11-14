package uk.huy.pathwise.planning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.web.AllowedWhenStoreReadOnly;
import uk.huy.pathwise.planning.dto.request.LocationBatchRequest;
import uk.huy.pathwise.planning.dto.request.UpdateLocationSharingRequest;
import uk.huy.pathwise.planning.service.DriverLocationService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

/**
 * The driver's position (API_SPEC §8, D-26, P-53): a DRIVER, or an OWNER/DISPATCHER with {@code canDrive} (D-21).
 */
// Drivers keep sending their location in a locked store, to finish running deliveries (T56, P-59).
@AllowedWhenStoreReadOnly
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driver/location")
@PreAuthorize("@authz.canDrive(principal)")
public class DriverLocationController {
    private final DriverLocationService driverLocationService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    public ResponseEntity<Void> recordLocations(@AuthenticationPrincipal AuthenticatedUser actor,
                                                @Valid @RequestBody LocationBatchRequest request) {
        driverLocationService.recordLocations(actor.identity(), request);
        return ResponseEntity.noContent().build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/sharing")
    public ResponseEntity<Void> updateSharing(@AuthenticationPrincipal AuthenticatedUser actor,
                                              @Valid @RequestBody UpdateLocationSharingRequest request) {
        driverLocationService.updateSharing(actor.identity(), request);
        return ResponseEntity.noContent().build();
    }
}
