package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.shared.identity.UserIdentity;

public record LogInResponse(IssuedAccessToken accessToken, RefreshTokenGenerationDetail refreshToken, UserIdentity user) {}
