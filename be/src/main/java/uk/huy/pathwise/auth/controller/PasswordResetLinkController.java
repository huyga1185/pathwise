package uk.huy.pathwise.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.huy.pathwise.auth.dto.request.ResetLinkOtpRequest;
import uk.huy.pathwise.auth.dto.request.ResetPasswordByLinkRequest;
import uk.huy.pathwise.auth.infrastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.service.PasswordResetLinkService;
import uk.huy.pathwise.core.web.AllowedWhenStoreReadOnly;

/**
 * The public page of a password reset link sent by an ADMIN (T57, P-63): ask a code, then set the password.
 */
@AllowedWhenStoreReadOnly
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/password/reset-link")
public class PasswordResetLinkController {
    private final PasswordResetLinkService passwordResetLinkService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/otp")
    public ResponseEntity<Void> sendOtp(@CurrentClient ClientDetail clientDetail, @Valid @RequestBody ResetLinkOtpRequest request) {
        passwordResetLinkService.sendOtp(clientDetail, request);
        return ResponseEntity.noContent().build();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordByLinkRequest request) {
        passwordResetLinkService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
