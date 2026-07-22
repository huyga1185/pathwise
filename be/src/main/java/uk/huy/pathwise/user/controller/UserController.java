package uk.huy.pathwise.user.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.shared.response.ApiResponse;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserProfileService;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserProfileService userService;

    @PutMapping("/me")
    public ResponseEntity<Void> updateUserProfile(@AuthenticationPrincipal UserIdentity identity,
                                                  @Valid @RequestBody UserModificationRequest request) {
        userService.updateUser(identity, request);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUserProfile(@AuthenticationPrincipal UserIdentity identity) {
        return ResponseEntity.ok().body(new ApiResponse<>(null, userService.getUser(identity)));
    }
}
