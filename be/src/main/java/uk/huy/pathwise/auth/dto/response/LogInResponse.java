package uk.huy.pathwise.auth.dto.response;

import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;

public record LogInResponse(String accessToken, RefreshTokenGenerationDetail refreshToken) {}
