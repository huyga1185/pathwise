package uk.huy.pathwise.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.request.UpdateCanDriveRequest;
import uk.huy.pathwise.store.dto.request.UpdateMemberRequest;
import uk.huy.pathwise.store.dto.response.MemberResponse;
import uk.huy.pathwise.store.service.MemberService;

import java.util.List;

/**
 * Members of the caller's store (API_SPEC §4). OWNER and DISPATCHER list them; only the OWNER changes them.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/store/members")
public class StoreMemberController {
    private final MemberService memberService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> listMembers(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                         @RequestParam(required = false) UserRole role,
                                                                         @RequestParam(required = false) UserStatus status,
                                                                         @RequestParam(defaultValue = "false") boolean assignable) {
        return ResponseEntity.ok(new ApiResponse<>(memberService.listMembers(actor.identity(), role, status, assignable)));
    }

    // Declared before /{userId} for readability; Spring prefers the literal segment "me" anyway.
    @PatchMapping("/me/can-drive")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MemberResponse>> updateOwnCanDrive(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                         @Valid @RequestBody UpdateCanDriveRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(memberService.updateOwnCanDrive(actor.identity(), request.canDrive())));
    }

    @PatchMapping("/{userId}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MemberResponse>> updateMember(@AuthenticationPrincipal AuthenticatedUser actor,
                                                                    @PathVariable("userId") long userId,
                                                                    @Valid @RequestBody UpdateMemberRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(memberService.updateMember(actor.identity(), userId, request)));
    }
}
