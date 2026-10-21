package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyOtpRequest(@NotBlank(message = "OTP Required!")
                               @Size(min = 6, max = 6, message = "OTP must contain 6 digits") String otp) {}
