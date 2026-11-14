package uk.huy.pathwise.admin.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /admin/stores/{id}/owner} (T57): the member who becomes the OWNER.
 */
public record TransferOwnerRequest(@NotNull(message = "USER_ID_REQUIRED") Long userId) {}
