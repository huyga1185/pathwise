package uk.huy.pathwise.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInvitationRequest(@NotBlank(message = "EMAIL_REQUIRED") @Email(message = "INVALID_EMAIL_FORMAT") String email,
                                      @NotNull(message = "ROLE_REQUIRED") InvitableRole role) {}
