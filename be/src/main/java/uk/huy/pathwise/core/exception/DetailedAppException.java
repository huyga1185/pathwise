package uk.huy.pathwise.core.exception;

import lombok.Getter;

import java.util.List;

/**
 * A business error that also tells the client which fields are wrong, returned as {@link ErrorResponse#details()}
 * (P-24: a subclass of {@link AppException} only when the error carries data for {@code details}). For example a
 * check that needs the database, such as a stop id that is not in the plan.
 */
@Getter
public class DetailedAppException extends AppException {
    private final List<FieldErrorDetail> details;

    public DetailedAppException(ErrorCode errorCode, List<FieldErrorDetail> details) {
        super(errorCode);
        this.details = List.copyOf(details);
    }
}
