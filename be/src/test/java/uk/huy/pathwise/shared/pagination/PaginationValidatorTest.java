package uk.huy.pathwise.shared.pagination;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.huy.pathwise.shared.exception.AppException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationValidatorTest {
    static final Pageable UNSORTED_PAGEABLE = PageRequest.of(1, 1, Sort.unsorted());
    static final Set<String> WHITELIST = Set.of("id");
    static final Pageable PAGEABLE = PageRequest.of(1, 1, Sort.by("id"));

    @Test
    void validateSort_PageableNull_throwNPE() {
        assertThrows(NullPointerException.class,
                () -> PaginationValidator.validateSort(null, WHITELIST));
    }

    @Test
    void validateSort_WhitelistNull_throwNPE() {
        assertThrows(NullPointerException.class,
                () -> PaginationValidator.validateSort(UNSORTED_PAGEABLE, null));
    }

    @Test
    void validateSort_WhitelistEmpty_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> PaginationValidator.validateSort(UNSORTED_PAGEABLE, new HashSet<>()));
    }

    @Test
    void validateSort_sortIsUnsorted_doesNotThrow() {
        assertDoesNotThrow(() -> PaginationValidator.validateSort(UNSORTED_PAGEABLE, WHITELIST));
    }

    @Test
    void validateSort_sortValid_doesNotThrow() {
        assertDoesNotThrow(() -> PaginationValidator.validateSort(PAGEABLE, WHITELIST));
    }

    @Test
    void validateSort_sortInvalid_throwAppException() {
        assertThrows(AppException.class,
                () -> PaginationValidator.validateSort(PAGEABLE, Set.of("invalid")));
    }

    @Test
    void validateSort_sortValidWithMultipleOrders_doesNotThrow() {
        assertDoesNotThrow(() -> PaginationValidator.validateSort(PageRequest.of(1, 1, Sort.by("field1", "field2", "field3")), Set.of("field1", "field2", "field3")));
    }

    @Test
    void validateSort_sortInvalidWithMultipleOrders_throwAppException() {
        assertThrows(AppException.class, () -> PaginationValidator.validateSort(PageRequest.of(1, 1, Sort.by("field1", "field2", "field3")), Set.of("field1", "field2")));
    }
}
