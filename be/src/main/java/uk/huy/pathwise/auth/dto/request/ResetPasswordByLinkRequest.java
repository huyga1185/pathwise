package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /auth/password/reset-link} (T57, P-63): the link's token, the code emailed for it, the new password.
 */
public record ResetPasswordByLinkRequest(@NotBlank(message = "RESET_TOKEN_REQUIRED") String token,
                                         @NotBlank(message = "OTP_REQUIRED") @Size(min = 6, max = 6, message = "INVALID_OTP_LENGTH") String otp,
                                         @NotBlank(message = "PASSWORD_REQUIRED")
                                         @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String newPassword) {}
