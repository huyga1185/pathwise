package uk.huy.pathwise.shared.pagination;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;

import java.util.Set;

public class PaginationValidator {
    public static void validateSort(Pageable pageable, Set<String> whitelist) {
        if (pageable == null) throw new NullPointerException("Pageable is NULL");
        if (whitelist == null) throw new NullPointerException("Whitelist is NULL");
        if (whitelist.isEmpty()) throw new IllegalArgumentException("Whitelist is empty");
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) return;
        for (Sort.Order order : sort) {
            if (!whitelist.contains(order.getProperty()))
                throw new AppException(ErrorCode.INVALID_SORT_FIELD);
        }
    }
}
