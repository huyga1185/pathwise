package uk.huy.pathwise.user.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.service.UserProfileService;

@RestController
@AllArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {
    private final UserProfileService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AdminGetUserResponse>>> getListUser(@PageableDefault(sort = "id") Pageable pageable) {
        return ResponseEntity.ok().body(new ApiResponse<>(null, userService.getListUser(pageable)));
    }
}
