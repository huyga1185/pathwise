package uk.huy.pathwise.core.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Runs {@link SelfValidating#validate} during Bean Validation, in the default group.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Constraint(validatedBy = SelfValidatedValidator.class)
public @interface SelfValidated {
    String message() default "INVALID";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
