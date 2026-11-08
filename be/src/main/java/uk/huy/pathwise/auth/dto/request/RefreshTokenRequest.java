package uk.huy.pathwise.auth.dto.request;

/**
 * Body of refresh and log-out for mobile clients ({@code X-Client-Platform: mobile}, P-04).
 */
public record RefreshTokenRequest(String refreshToken) {}
