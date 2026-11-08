package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RequestPasswordChangeOtpRequest(@NotBlank(message = "CURRENT_PASSWORD_REQUIRED") String currentPassword) {}
