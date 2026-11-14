package uk.huy.pathwise.store.service;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.huy.pathwise.store.dto.StoreRecord;
import uk.huy.pathwise.shared.store.StoreStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Every store, for the {@code admin} module (T55, P-60). Not tenant-scoped: only ADMIN endpoints may use it.
 */
public interface StoreDirectoryQuery {
    /**
     * @param text          matched, case-insensitively and literally, against the store name; null or blank: every
     *                      store
     * @param alsoStoreIds  stores to include whatever their name (the stores whose OWNER email matched {@code text})
     * @param status        only stores in this state; null: any
     * @param pageable      sort by {@code id}, {@code name} or {@code createdAt}
     * @throws uk.huy.pathwise.core.exception.AppException {@code INVALID_SORT_FIELD} for another sort
     */
    Page<StoreRecord> search(@Nullable String text, Collection<Long> alsoStoreIds, @Nullable StoreStatus status,
                             Pageable pageable);

    Optional<StoreRecord> find(long storeId);

    /**
     * @return stores created in {@code [from, to)}, by local day at {@code offset} (T60); days without any are absent
     */
    Map<LocalDate, Long> countCreatedByDay(Instant from, Instant to, ZoneOffset offset);

    /**
     * @return every store, and those READ_ONLY (T60)
     */
    StoreTotals totals();

    record StoreTotals(long stores, long readOnly) {}
}
