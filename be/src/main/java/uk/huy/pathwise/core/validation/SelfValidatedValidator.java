package uk.huy.pathwise.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SelfValidatedValidator implements ConstraintValidator<SelfValidated, SelfValidating> {
    @Override
    public boolean isValid(SelfValidating value, ConstraintValidatorContext context) {
        if (value == null) return true;
        FieldErrors errors = new FieldErrors();
        value.validate(errors);
        if (errors.rejections().isEmpty()) return true;
        context.disableDefaultConstraintViolation();
        for (FieldErrors.Rejection rejection : errors.rejections()) {
            context.buildConstraintViolationWithTemplate(rejection.code())
                    .addPropertyNode(rejection.field())
                    .addConstraintViolation();
        }
        return false;
    }
}
