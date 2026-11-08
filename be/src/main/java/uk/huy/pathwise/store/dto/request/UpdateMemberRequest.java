package uk.huy.pathwise.store.dto.request;

import lombok.Getter;
import uk.huy.pathwise.core.validation.FieldErrors;
import uk.huy.pathwise.core.validation.MergePatch;

/**
 * {@code PATCH /store/members/{userId}} as a merge patch ({@link MergePatch}): an absent field is left unchanged. No
 * field can be cleared, so {@code null} is 400 {@code NOT_NULLABLE}.
 */
@Getter
public class UpdateMemberRequest extends MergePatch {
    private MemberStatusChange status;
    private InvitableRole role;
    private Boolean canDrive;

    @Override
    public void validate(FieldErrors errors) {
        rejectNull(errors, "status", status);
        rejectNull(errors, "role", role);
        rejectNull(errors, "canDrive", canDrive);
    }

    public void setStatus(MemberStatusChange status) {
        this.status = status;
        present("status");
    }

    public void setRole(InvitableRole role) {
        this.role = role;
        present("role");
    }

    public void setCanDrive(Boolean canDrive) {
        this.canDrive = canDrive;
        present("canDrive");
    }
}
