package uk.huy.pathwise.auth.resetlink;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.statestore.RedisStateStore;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Keys {@code resetLink:{sha256(token)}} → userId and {@code resetLink:user:{userId}} → sha256(token), both with the
 * link's TTL (DATA_MODEL §3). The second key lets a new link replace the previous one.
 */
@Component
public class RedisResetLinkStore extends RedisStateStore<String, String> implements ResetLinkStore {
    public RedisResetLinkStore(StringRedisTemplate redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public void save(long userId, String tokenHash, Duration ttl) {
        find(userKey(userId)).ifPresent(previous -> delete(linkKey(previous)));
        save(linkKey(tokenHash), String.valueOf(userId), ttl);
        save(userKey(userId), tokenHash, ttl);
    }

    @Override
    public Optional<Long> findUserId(String tokenHash) {
        return find(linkKey(tokenHash)).map(Long::valueOf);
    }

    @Override
    public void delete(long userId, String tokenHash) {
        delete(linkKey(tokenHash));
        if (Objects.equals(find(userKey(userId)).orElse(null), tokenHash)) delete(userKey(userId));
    }

    private static String linkKey(String tokenHash) {
        return "resetLink:" + tokenHash;
    }

    private static String userKey(long userId) {
        return "resetLink:user:" + userId;
    }
}
