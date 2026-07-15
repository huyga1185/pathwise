package uk.huy.pathwise.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreationRequest(
        @NotBlank(message = "Email could not be empty") @Email(message = "Email invalid") String email,
        @NotBlank(message = "Password could not be empty") @Size(min = 8, max = 255, message = "Password length invalid") String password,
        String phoneNumber) {}
