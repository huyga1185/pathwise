package uk.huy.pathwise.core.validation;

import java.util.HashSet;
import java.util.Set;

/**
 * Base of every PATCH request (API_SPEC §1, JSON Merge Patch, RFC 7396): a field absent from the JSON keeps its value,
 * a field present with {@code null} clears it, and a field that cannot be cleared answers 400 {@code NOT_NULLABLE}.
 * <p>
 * A class with setters rather than a record, because a record cannot tell an absent field from a {@code null} one:
 * Jackson calls a setter only for fields present in the JSON. Each setter of a subclass calls {@link #present}.
 */
@SelfValidated
public abstract class MergePatch implements SelfValidating {
    public static final String NOT_NULLABLE = "NOT_NULLABLE";

    private final Set<String> present = new HashSet<>();

    /**
     * @return true if the field was in the JSON, even with {@code null}
     */
    public boolean has(String field) {
        return present.contains(field);
    }

    protected void present(String field) {
        present.add(field);
    }

    /**
     * Rejects a field that is present with {@code null} although it cannot be cleared.
     */
    protected void rejectNull(FieldErrors errors, String field, Object value) {
        if (has(field) && value == null) errors.reject(field, NOT_NULLABLE);
    }

    /**
     * Nothing to check here; subclasses override with their own rules.
     */
    @Override
    public void validate(FieldErrors errors) {}
}
