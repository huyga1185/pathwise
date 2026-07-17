package uk.huy.pathwise.user.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.shared.response.ApiResponse;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.service.UserProfileService;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserProfileService userService;

    // todo changes id to me when using jwt

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUserProfile(@PathVariable long id,
                                                  @Valid @RequestBody UserModificationRequest request) {
        userService.updateUser(id, request);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetUserResponse>> getUserProfile(@PathVariable long id) {
        return ResponseEntity.ok().body(new ApiResponse<>(null, userService.getUser(id)));
    }
}
