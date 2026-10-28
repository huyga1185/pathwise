package uk.huy.pathwise.core.exception;

import lombok.Getter;

import java.util.List;

/**
 * A business error that also tells the client which fields are wrong, returned as {@link ErrorResponse#details()}
 * (P-24: a subclass of {@link AppException} only when the error carries data for {@code details}). For example a
 * check that needs the database, such as a stop id that is not in the plan. Each detail is a {@link FieldErrorDetail}
 * or, when the error code adds data for the client (API_SPEC §1), a record with {@code field}, {@code code} and those
 * extra fields.
 */
@Getter
public class DetailedAppException extends AppException {
    private final List<?> details;

    public DetailedAppException(ErrorCode errorCode, List<?> details) {
        super(errorCode);
        this.details = List.copyOf(details);
    }
}
