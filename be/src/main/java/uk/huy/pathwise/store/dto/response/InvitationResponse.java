package uk.huy.pathwise.store.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.store.model.InvitationStatus;

import java.time.Instant;

/**
 * @param status {@code EXPIRED} for a pending invitation past {@code expiresAt}
 */
public record InvitationResponse(long id, String email, UserRole role, InvitationStatus status, Instant expiresAt) {}
