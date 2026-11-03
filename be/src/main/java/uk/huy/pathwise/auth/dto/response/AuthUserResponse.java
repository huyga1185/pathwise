package uk.huy.pathwise.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.identity.UserRole;

/**
 * {@code user} of the log-in response (API_SPEC §2). {@code fullName} is omitted when not set, {@code storeId} for
 * ADMIN (no store).
 *
 * @param canDrive true if the user can be assigned routes: always for DRIVER, otherwise the stored flag (D-21)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthUserResponse(long id, String email, @Nullable String fullName, UserRole role, @Nullable Long storeId, boolean canDrive) {}
