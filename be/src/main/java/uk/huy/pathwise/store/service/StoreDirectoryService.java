package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.pagination.PaginationValidator;
import uk.huy.pathwise.core.search.LikePattern;
import uk.huy.pathwise.store.dto.StoreRecord;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.model.StoreStatus;
import uk.huy.pathwise.store.repository.StoreRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreDirectoryService implements StoreDirectoryQuery {
    private static final Set<String> SORT_WHITELIST = Set.of("id", "name", "createdAt");
    // An IN list that matches no store, as an empty list is not portable in JPQL.
    private static final List<Long> NO_STORE = List.of(-1L);

    private final StoreRepository storeRepository;

    @Override
    public Page<StoreRecord> search(@Nullable String text, Collection<Long> alsoStoreIds, @Nullable StoreStatus status,
                                    Pageable pageable) {
        PaginationValidator.validateSort(pageable, SORT_WHITELIST);
        boolean filtered = text != null && !text.isBlank();
        String pattern = filtered ? LikePattern.contains(text) : null;
        Collection<Long> ids = alsoStoreIds.isEmpty() ? NO_STORE : alsoStoreIds;
        return storeRepository.search(filtered, pattern, ids, status, pageable).map(StoreDirectoryService::toRecord);
    }

    @Override
    public Optional<StoreRecord> find(long storeId) {
        return storeRepository.findById(storeId).map(StoreDirectoryService::toRecord);
    }

    private static StoreRecord toRecord(Store store) {
        return new StoreRecord(store.getId(), store.getName(), store.getStatus(), store.getTimezone(),
                store.getLockReason(), store.getLockedAt(), store.getCreatedAt());
    }
}
