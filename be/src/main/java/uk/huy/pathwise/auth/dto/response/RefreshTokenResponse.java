package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.jwt.dto.IssuedAccessToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult;

public record RefreshTokenResponse(IssuedAccessToken accessToken, RotationResult.Rotated refreshToken) {}
