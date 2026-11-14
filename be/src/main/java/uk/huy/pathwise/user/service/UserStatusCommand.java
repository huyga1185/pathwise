package uk.huy.pathwise.user.service;

import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;

/**
 * Disables and enables any account, for the {@code admin} module (T56). Business rules (an ADMIN cannot disable
 * themself) belong to the caller; sessions are revoked by the caller too.
 */
public interface UserStatusCommand {
    /**
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if there is no such user
     */
    AdminGetUserResponse disable(long userId);

    /**
     * Back to ACTIVE, or to PENDING_VERIFICATION if the email was never verified.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if there is no such user
     */
    AdminGetUserResponse enable(long userId);
}
