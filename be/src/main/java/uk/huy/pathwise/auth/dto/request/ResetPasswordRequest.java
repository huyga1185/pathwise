package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank(message = "RESET_TOKEN_REQUIRED") String resetToken,
                                   @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String newPassword) {}
