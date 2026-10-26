package uk.huy.pathwise.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank(message = "EMAIL_REQUIRED") @Email(message = "INVALID_EMAIL_FORMAT") String email,
                              @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String password,
                              @NotBlank(message = "STORE_NAME_REQUIRED") @Size(max = 150, message = "INVALID_STORE_NAME_LENGTH") String storeName,
                              @Size(max = 100, message = "INVALID_FULL_NAME_LENGTH") String fullName) {}
