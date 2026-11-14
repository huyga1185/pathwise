package uk.huy.pathwise.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.admin.service.AdminUserService;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;

/**
 * Account actions of the platform ADMIN (T56, FR-A12). The list stays at {@code GET /admin/users} (module user).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserActionController {
    private final AdminUserService adminUserService;

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<AdminGetUserResponse>> disable(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                     @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(adminUserService.disable(actor.identity(), id)));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<AdminGetUserResponse>> enable(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                    @PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(adminUserService.enable(actor.identity(), id)));
    }

    @PostMapping("/{id}/sessions/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> revokeSessions(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        adminUserService.revokeSessions(actor.identity(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/verification-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> sendVerificationEmail(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        adminUserService.sendVerificationEmail(actor.identity(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/password-reset-email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> sendPasswordResetLink(@AuthenticationPrincipal AuthenticatedUser actor, @PathVariable long id) {
        adminUserService.sendPasswordResetLink(actor.identity(), id);
        return ResponseEntity.noContent().build();
    }
}
