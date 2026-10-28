package uk.huy.pathwise.user.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import uk.huy.pathwise.core.phonenumber.ValidPhoneNumber;
import uk.huy.pathwise.core.validation.FieldErrors;
import uk.huy.pathwise.core.validation.MergePatch;

/**
 * {@code PATCH /users/me} as a merge patch ({@link MergePatch}): an absent field is left unchanged, {@code null}
 * clears it. The phone number is a Vietnamese number, stored and returned as E.164.
 */
@Getter
public class UpdateProfileRequest extends MergePatch {
    @Size(max = 100, message = "INVALID_FULL_NAME_LENGTH")
    private String fullName;

    @ValidPhoneNumber
    private String phoneNumber;

    @Override
    public void validate(FieldErrors errors) {
        // Clearing is null; a blank name is a mistake rather than a way to clear it.
        if (fullName != null && fullName.isBlank()) errors.reject("fullName", "FULL_NAME_REQUIRED");
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
        present("fullName");
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        present("phoneNumber");
    }
}
