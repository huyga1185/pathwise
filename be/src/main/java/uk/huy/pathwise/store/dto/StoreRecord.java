package uk.huy.pathwise.store.dto;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.store.model.StoreStatus;

import java.time.Instant;

/**
 * A store as the ADMIN area sees it (T55): its state, not its content (no depot, P-59).
 *
 * @param lockReason why it is READ_ONLY (T56); null while ACTIVE
 * @param lockedAt   when it was locked; null while ACTIVE
 */
public record StoreRecord(long id,
                          String name,
                          StoreStatus status,
                          String timezone,
                          @Nullable String lockReason,
                          @Nullable Instant lockedAt,
                          Instant createdAt) {}
