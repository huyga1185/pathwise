package uk.huy.pathwise.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Body of log-in ({@code LoginResponse}) and refresh ({@code TokenResponse}) in API_SPEC §2.
 * {@code refreshToken} is only set for mobile clients; {@code user} only on log-in.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthTokenResponse(String accessToken,
                                Instant accessTokenExpiresAt,
                                String refreshToken,
                                Instant refreshTokenExpiresAt,
                                AuthUserResponse user) {}
