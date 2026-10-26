package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailRequest(@NotBlank(message = "EMAIL_REQUIRED") @Email(message = "INVALID_EMAIL_FORMAT") String email,
                                 @NotBlank(message = "OTP_REQUIRED") @Size(min = 6, max = 6, message = "INVALID_OTP_LENGTH") String otp) {}
