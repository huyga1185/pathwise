package uk.huy.pathwise.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.store.dto.request.CreateInvitationRequest;
import uk.huy.pathwise.store.dto.response.InvitationResponse;
import uk.huy.pathwise.store.model.InvitationStatus;
import uk.huy.pathwise.store.service.InvitationService;

import java.util.List;

/**
 * Invitations of the caller's store (API_SPEC §4). OWNER only.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/store/invitations")
@PreAuthorize("hasRole('OWNER')")
public class StoreInvitationController {
    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<ApiResponse<InvitationResponse>> createInvitation(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                            @Valid @RequestBody CreateInvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(invitationService.createInvitation(actor.identity(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> listInvitations(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                                 @RequestParam(required = false) InvitationStatus status) {
        return ResponseEntity.ok(new ApiResponse<>(invitationService.listInvitations(actor.identity(), status)));
    }

    /**
     * Revokes a PENDING invitation; it stays listed with status REVOKED.
     */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<Void> revokeInvitation(@AuthenticationPrincipal AuthenticatedUser actor,
                                                 @PathVariable("id") long invitationId) {
        invitationService.revokeInvitation(actor.identity(), invitationId);
        return ResponseEntity.noContent().build();
    }
}
