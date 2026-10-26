package uk.huy.pathwise.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.store.dto.request.UpdateStoreRequest;
import uk.huy.pathwise.store.dto.response.StoreResponse;
import uk.huy.pathwise.store.service.StoreService;

/**
 * The caller's own store; there is no store id in the path (API_SPEC §4). Role checks come in T17; until then any
 * logged-in member of the store may call these endpoints.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/store")
public class StoreController {
    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.getStore(actor.identity())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                  @Valid @RequestBody UpdateStoreRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(storeService.updateStore(actor.identity(), request)));
    }
}
