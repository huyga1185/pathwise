package uk.huy.pathwise.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code POST /admin/stores/{id}/lock} (T56): the reason is shown to the store's members in the lock banner.
 */
public record LockStoreRequest(@NotBlank(message = "LOCK_REASON_REQUIRED")
                               @Size(max = 500, message = "INVALID_LOCK_REASON_LENGTH") String reason) {}
