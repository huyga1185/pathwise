package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;

public record RefreshTokenResponse(String accessToken, RefreshTokenRotationDetail refreshTokenRotationDetail) {}
