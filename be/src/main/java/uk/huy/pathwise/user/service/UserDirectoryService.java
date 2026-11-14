package uk.huy.pathwise.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.search.LikePattern;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.dto.MemberAccount;
import uk.huy.pathwise.user.repository.UserRepository;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDirectoryService implements UserDirectoryQuery {
    private final UserRepository userRepository;

    @Override
    public Set<Long> findStoreIdsByOwnerEmail(String text) {
        return new HashSet<>(userRepository.findOwnerStoreIdsByEmailLike(LikePattern.contains(text)));
    }

    @Override
    public Map<Long, String> findOwnerEmails(Collection<Long> storeIds) {
        Map<Long, String> emails = new HashMap<>();
        if (storeIds.isEmpty()) return emails;
        for (Object[] row : userRepository.findOwnerEmails(storeIds)) emails.put((Long) row[0], (String) row[1]);
        return emails;
    }

    @Override
    public Map<Long, Map<UserRole, Long>> countMembersByRole(Collection<Long> storeIds) {
        Map<Long, Map<UserRole, Long>> counts = new HashMap<>();
        if (storeIds.isEmpty()) return counts;
        for (Object[] row : userRepository.countByStoreAndRole(storeIds)) {
            counts.computeIfAbsent((Long) row[0], id -> new EnumMap<>(UserRole.class)).put((UserRole) row[1], (Long) row[2]);
        }
        return counts;
    }

    @Override
    public Map<UserRole, Long> countByRole() {
        Map<UserRole, Long> counts = new EnumMap<>(UserRole.class);
        for (UserRole role : UserRole.values()) counts.put(role, 0L);
        for (Object[] row : userRepository.countByRole()) counts.put((UserRole) row[0], (Long) row[1]);
        return counts;
    }

    @Override
    public List<MemberAccount> listMembers(long storeId) {
        return userRepository.findAllByStoreIdOrderById(storeId).stream()
                .map(user -> new MemberAccount(user.getId(), user.getEmail(), user.getRole(), user.getStatus(),
                        user.getCreatedAt()))
                .toList();
    }
}
