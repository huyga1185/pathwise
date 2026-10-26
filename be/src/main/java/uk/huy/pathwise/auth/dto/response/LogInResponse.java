package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.user.dto.AuthenticationResult.Authenticated;

public record LogInResponse(IssuedAccessToken accessToken, RefreshTokenGenerationDetail refreshToken, Authenticated user) {}
