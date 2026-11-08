package uk.huy.pathwise.store.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

public record InvitationPreviewResponse(String storeName, String email, UserRole role, Instant expiresAt) {}
