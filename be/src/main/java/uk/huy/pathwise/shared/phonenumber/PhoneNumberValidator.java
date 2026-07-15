package uk.huy.pathwise.shared.phonenumber;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    private final PhoneNumberService phoneNumberService;

    public PhoneNumberValidator(PhoneNumberService phoneNumberService) {
        this.phoneNumberService = phoneNumberService;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        PhoneCheck phoneCheck = phoneNumberService.check(value);
        if (phoneCheck == PhoneCheck.VALID) return true;
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(switch(phoneCheck) {
            case WRONG_REGION -> "PHONE_NUMBER_WRONG_REGION";
            case TOO_LONG -> "PHONE_NUMBER_TOO_LONG";
            case TOO_SHORT -> "PHONE_NUMBER_TOO_SHORT";
            case WRONG_PHONE_NUMBER_TYPE -> "PHONE_NUMBER_TYPE_NOT_ALLOWED";
            default -> "PHONE_NUMBER_INVALID";
        }).addConstraintViolation();
        return false;
    }
}
