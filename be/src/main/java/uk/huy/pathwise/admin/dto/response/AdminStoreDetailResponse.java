package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.store.model.StoreStatus;

import java.time.Instant;
import java.util.List;

/**
 * One store for the ADMIN (T55): the totals of the list, the last 30 days and the members. No plan, vehicle, stop or
 * recipient content (P-59).
 *
 * @param lockReason why the store is READ_ONLY (T56); null while ACTIVE
 * @param lockedAt   when it was locked; null while ACTIVE
 */
public record AdminStoreDetailResponse(long id,
                                       String name,
                                       @Nullable String ownerEmail,
                                       StoreStatus status,
                                       @Nullable String lockReason,
                                       @Nullable Instant lockedAt,
                                       String timezone,
                                       Instant createdAt,
                                       MemberCountsResponse memberCounts,
                                       long vehicles,
                                       long plans,
                                       @Nullable Instant lastActivityAt,
                                       StorePeriodStatsResponse last30Days,
                                       List<AdminMemberResponse> members) {}
