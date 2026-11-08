package uk.huy.pathwise.core.validation;

/**
 * A request DTO that checks rules across its own fields (for example {@code start < end}). Annotate the type with
 * {@link SelfValidated}: unlike {@code @AssertTrue} on a method, whose error names the method, each error then names the
 * field the client must fix (API_SPEC §1).
 */
public interface SelfValidating {
    void validate(FieldErrors errors);
}
