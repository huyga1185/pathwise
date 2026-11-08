package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendVerificationRequest(@NotBlank(message = "EMAIL_REQUIRED") @Email(message = "INVALID_EMAIL_FORMAT") String email) {}
