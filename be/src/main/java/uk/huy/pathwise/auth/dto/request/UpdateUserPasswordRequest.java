package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserPasswordRequest(@NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String password) {}
