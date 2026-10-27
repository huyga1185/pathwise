package uk.huy.pathwise.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param phoneNumber optional Vietnamese number; an invalid one gets 400 {@code INVALID_PHONE_NUMBER}
 */
public record AcceptInvitationRequest(@NotBlank(message = "INVITATION_CODE_REQUIRED") String code,
                                      @NotBlank(message = "PASSWORD_REQUIRED") @Size(min = 8, max = 72, message = "INVALID_PASSWORD_LENGTH") String password,
                                      @NotBlank(message = "FULL_NAME_REQUIRED") @Size(max = 100, message = "INVALID_FULL_NAME_LENGTH") String fullName,
                                      @Size(max = 20, message = "PHONE_NUMBER_TOO_LONG") String phoneNumber) {}
