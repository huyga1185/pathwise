package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.store.StoreStatus;

import java.time.Instant;

/**
 * A store in the ADMIN list (T55): totals only, no content (P-59).
 *
 * @param ownerEmail     null only for a store without OWNER (should not happen, BR-03)
 * @param vehicles       active vehicles
 * @param plans          plans of any status
 * @param lastActivityAt latest plan created or changed, or stop result reported (P-60); null without plans
 */
public record AdminStoreSummaryResponse(long id,
                                        String name,
                                        @Nullable String ownerEmail,
                                        StoreStatus status,
                                        Instant createdAt,
                                        MemberCountsResponse memberCounts,
                                        long vehicles,
                                        long plans,
                                        @Nullable Instant lastActivityAt) {}
