package uk.huy.pathwise.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ErrorCode {
    EMAIL_EXISTED(409),
    INVALID_DEPOT( 400),
    INVALID_EMAIL_OR_PASSWORD(401),
    INVALID_SORT_FIELD(400),
    INVALID_PHONE_NUMBER(400),
    INVALID_TOKEN(401),
    INVALID_OTP(400),
    OTP_COOLDOWN(429),
    /** @deprecated no longer used since the old change-password flow was removed (T15); kept because error codes are only ever added. */
    @Deprecated
    COULD_NOT_UPDATE_USER_PASSWORD(400),
    COULD_NOT_FIND_CHEAPEST_ROUTE(400),
    COULD_NOT_FIND_ADDRESS(400),
    /** @deprecated replaced by {@link #USER_NOT_FOUND}; kept because error codes are only ever added. */
    @Deprecated
    COULD_NOT_FIND_USER(404),
    INTERNAL_ERROR(500),
    VALIDATION_ERROR(400),
    MALFORMED_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    USER_NOT_FOUND(404),
    SESSION_NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    GEOCODING_PROVIDER_ERROR(502),
    ROUTING_PROVIDER_ERROR(502),
    EMAIL_NOT_VERIFIED(403),
    ACCOUNT_DISABLED(403),
    EMAIL_ALREADY_VERIFIED(409),
    INVALID_TIMEZONE(400),
    INVALID_RESET_TOKEN(400),
    INVALID_CURRENT_PASSWORD(400),
    CANNOT_MODIFY_OWNER(422),
    INVITATION_INVALID(404),
    INVITATION_NOT_PENDING(422),
    GEOCODING_BUSY(503),
    VEHICLE_NAME_EXISTED(409),
    PLAN_MODIFIED(409),
    PLAN_BUSY(409),
    PLAN_NOT_EDITABLE(422),
    PLAN_TOO_LARGE(422),
    STORE_DEPOT_REQUIRED(422),
    INVALID_ASSIGNMENT(422),
    JOB_NOT_FOUND(404),
    NOTHING_TO_GEOCODE(422);
    private final int httpCode;
}
