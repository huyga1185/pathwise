package uk.huy.pathwise.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code user} of the log-in response (API_SPEC §2). {@code fullName} is omitted when not set, {@code storeId} for
 * ADMIN (no store).
 *
 * @param canDrive true if the user can be assigned routes: always for DRIVER, otherwise the stored flag (D-21)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthUserResponse(long id, String email, String fullName, String role, Long storeId, boolean canDrive) {}
