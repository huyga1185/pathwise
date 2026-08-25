package uk.huy.pathwise.auth.token.jwt.infrastructure.exception;

public class JwtException extends RuntimeException {
    public JwtException(String message) {
        super(message);
    }
    public JwtException(String message, Throwable e) {
        super(message, e);
    }
}
