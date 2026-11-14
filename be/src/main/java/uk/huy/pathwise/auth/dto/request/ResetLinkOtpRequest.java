package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * {@code POST /auth/password/reset-link/otp} (T57): the token of the link an ADMIN sent.
 */
public record ResetLinkOtpRequest(@NotBlank(message = "RESET_TOKEN_REQUIRED") String token) {}
