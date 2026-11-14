package uk.huy.pathwise.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.admin.dto.request.LockStoreRequest;
import uk.huy.pathwise.admin.dto.response.AdminStoreDetailResponse;
import uk.huy.pathwise.admin.dto.response.AdminStoreStateResponse;
import uk.huy.pathwise.admin.dto.response.AdminStoreSummaryResponse;
import uk.huy.pathwise.admin.service.AdminStoreService;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.store.StoreStatus;

/**
 * Stores for the platform ADMIN (T55, FR-A11). Totals and member accounts only (P-59).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stores")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStoreController {
    private final AdminStoreService adminStoreService;

    /**
     * @param q matched against the store name and the OWNER email (case-insensitive, contains)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminStoreSummaryResponse>>> listStores(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) StoreStatus status,
            @ParameterObject @PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new ApiResponse<>(null, adminStoreService.listStores(q, status, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminStoreDetailResponse>> getStore(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                          @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(null, adminStoreService.getStore(actor.identity(), id)));
    }

    /**
     * Locks the store READ_ONLY (T56); locking again replaces the reason.
     */
    @PostMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<AdminStoreStateResponse>> lock(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                     @PathVariable long id,
                                                                     @Valid @RequestBody LockStoreRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(adminStoreService.lock(actor.identity(), id, request.reason())));
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<AdminStoreStateResponse>> unlock(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                       @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(adminStoreService.unlock(actor.identity(), id)));
    }
}
