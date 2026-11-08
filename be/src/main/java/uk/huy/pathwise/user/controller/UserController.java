package uk.huy.pathwise.user.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.user.dto.request.UpdateProfileRequest;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.user.service.UserProfileService;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserProfileService userService;

    /**
     * Merge patch: an absent field keeps its value, {@code null} clears it ({@link UpdateProfileRequest}).
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<GetUserResponse>> updateUserProfile(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                          @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(userService.updateUser(actor.identity(), request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUserProfile(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok().body(new ApiResponse<>(null, userService.getUser(actor.identity())));
    }
}
