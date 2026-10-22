package uk.huy.pathwise.user.infrastructure.phonenumber;

public enum PhoneCheck {
    VALID,
    NOT_A_PHONE_NUMBER,
    TOO_SHORT,
    TOO_LONG,
    WRONG_REGION,
    WRONG_PHONE_NUMBER_TYPE;
}
