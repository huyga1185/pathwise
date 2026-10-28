package uk.huy.pathwise.core.validation;

import java.util.ArrayList;
import java.util.List;

/**
 * Errors found by {@link SelfValidating#validate}, each on the field the client must fix.
 */
public final class FieldErrors {
    record Rejection(String field, String code) {}

    private final List<Rejection> rejections = new ArrayList<>();

    /**
     * @param field the request field to point at, relative to the validated object (e.g. {@code "end"})
     * @param code  UPPER_SNAKE error code
     */
    public void reject(String field, String code) {
        rejections.add(new Rejection(field, code));
    }

    List<Rejection> rejections() {
        return rejections;
    }
}
