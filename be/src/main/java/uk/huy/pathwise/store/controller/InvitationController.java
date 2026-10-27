package uk.huy.pathwise.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.store.dto.request.AcceptInvitationRequest;
import uk.huy.pathwise.store.dto.response.InvitationPreviewResponse;
import uk.huy.pathwise.store.service.InvitationService;

/**
 * Public endpoints for the invitee, who has no account yet (API_SPEC §4).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<InvitationPreviewResponse>> previewInvitation(@PathVariable("code") String code) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new ApiResponse<>(invitationService.previewInvitation(code)));
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        invitationService.acceptInvitation(request);
        return ResponseEntity.noContent().build();
    }
}
