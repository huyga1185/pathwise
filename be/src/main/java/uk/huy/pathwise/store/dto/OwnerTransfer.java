package uk.huy.pathwise.store.dto;

import org.jspecify.annotations.Nullable;

/**
 * Result of a change of OWNER (T57). Ids and emails only, for the ADMIN area and the audit log.
 *
 * @param previousOwnerId null only for a store that had no OWNER (should not happen, BR-03)
 */
public record OwnerTransfer(long storeId,
                            @Nullable Long previousOwnerId,
                            @Nullable String previousOwnerEmail,
                            long newOwnerId,
                            String newOwnerEmail,
                            boolean newOwnerCanDrive) {}
