package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.shared.identity.UserStatus;

public record RegisterResponse(String email, UserStatus status) {}
