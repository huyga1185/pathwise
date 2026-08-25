package uk.huy.pathwise.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    EMAIL_EXISTED(400),
    INVALID_DEPOT( 400),
    INVALID_EMAIL_OR_PASSWORD(400),
    INVALID_SORT_FIELD(400),
    INVALID_PHONE_NUMBER(400),
    INVALID_TOKEN(401),
    INVALID_OTP(400),
    OTP_COOLDOWN(429),
    COULD_NOT_UPDATE_USER_PASSWORD(400),
    COULD_NOT_FIND_CHEAPEST_ROUTE(400),
    COULD_NOT_FIND_ADDRESS(400),
    COULD_NOT_FIND_USER(400),
    SERVER_ERROR( 500);
    private final int httpCode;
}
