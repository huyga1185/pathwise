package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(@NotBlank(message = "OTP_REQUIRED") @Size(min = 6, max = 6, message = "INVALID_OTP_LENGTH") String otp,
                                    @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String newPassword) {}
