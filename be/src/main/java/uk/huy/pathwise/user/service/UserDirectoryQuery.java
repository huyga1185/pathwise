package uk.huy.pathwise.user.service;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.dto.MemberAccount;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Users across stores, for the {@code admin} module (T55, P-60). Not tenant-scoped: only ADMIN endpoints may use it.
 */
public interface UserDirectoryQuery {
    /**
     * @return the stores whose OWNER email contains {@code text} (case-insensitive, literal)
     */
    Set<Long> findStoreIdsByOwnerEmail(String text);

    /**
     * @return the OWNER email of each of {@code storeIds} that has one
     */
    Map<Long, String> findOwnerEmails(Collection<Long> storeIds);

    /**
     * @return for each of {@code storeIds} with members, the number of members by role, whatever their status
     */
    Map<Long, Map<UserRole, Long>> countMembersByRole(Collection<Long> storeIds);

    /**
     * @return the members of the store, by id
     */
    List<MemberAccount> listMembers(long storeId);

    /**
     * @return every user of the platform by role, whatever their status (T60)
     */
    Map<UserRole, Long> countByRole();
}
