package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;

/**
 * After a change of OWNER (T57): the previous OWNER is now a DISPATCHER, logged out everywhere.
 *
 * @param ownerCanDrive      true when the new OWNER can be assigned routes (always for a former DRIVER, P-63)
 * @param previousOwnerId    null only for a store that had no OWNER
 * @param previousOwnerEmail null only for a store that had no OWNER
 */
public record TransferOwnerResponse(long storeId,
                                    long ownerId,
                                    String ownerEmail,
                                    boolean ownerCanDrive,
                                    @Nullable Long previousOwnerId,
                                    @Nullable String previousOwnerEmail) {}
