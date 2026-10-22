package uk.huy.pathwise.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * {@code user} of the log-in response (API_SPEC §2). {@code fullName}, {@code storeId} and {@code canDrive}
 * stay null, and are therefore omitted, until users get those fields (T13).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthUserResponse(long id, String email, String fullName, String role, Long storeId, Boolean canDrive) {}
