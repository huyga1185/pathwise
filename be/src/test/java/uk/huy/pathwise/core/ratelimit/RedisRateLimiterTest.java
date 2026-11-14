package uk.huy.pathwise.core.ratelimit;

import org.springframework.beans.factory.ObjectProvider;
import uk.huy.pathwise.core.observability.OpsMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.core.statestore.StateStoreException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private Hasher hasher;
    @Mock
    private ObjectProvider<OpsMetrics> metricsProvider;
    @Mock
    private OpsMetrics opsMetrics;

    private final RateLimitProperties properties = new RateLimitProperties(
            Map.of("log-in-email", new RateLimitProperties.Limit(2, Duration.ofMinutes(15))));

    private RedisRateLimiter rateLimiter() {
        return new RedisRateLimiter(redisTemplate, properties, hasher, metricsProvider);
    }

    @Test
    void tryAcquire_whenAtLimit_allow() {
        when(hasher.hashString("a@pathwise.test")).thenReturn("hash");
        stubScript(List.of(2L, 899_000L));

        assertThat(rateLimiter().tryAcquire("log-in-email", "a@pathwise.test")).isInstanceOf(RateLimitDecision.Allowed.class);
    }

    @Test
    void tryAcquire_whenOverLimit_limitedWithTimeLeftInWindow() {
        when(hasher.hashString("a@pathwise.test")).thenReturn("hash");
        stubScript(List.of(3L, 120_500L));

        assertThat(rateLimiter().tryAcquire("log-in-email", "a@pathwise.test"))
                .isEqualTo(new RateLimitDecision.Limited(Duration.ofMillis(120_500)));
    }

    @Test
    void tryAcquire_whenOverLimit_countItForTheAdminMetrics() {
        when(hasher.hashString("a@pathwise.test")).thenReturn("hash");
        when(metricsProvider.getIfAvailable()).thenReturn(opsMetrics);
        stubScript(List.of(3L, 120_500L));

        rateLimiter().tryAcquire("log-in-email", "a@pathwise.test");

        verify(opsMetrics).recordRateLimited();
    }

    @Test
    void tryAcquire_whenRedisDown_throwStateStoreException() {
        when(hasher.hashString("a@pathwise.test")).thenReturn("hash");
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThatThrownBy(() -> rateLimiter().tryAcquire("log-in-email", "a@pathwise.test"))
                .isInstanceOf(StateStoreException.class);
    }

    @Test
    void tryAcquire_whenRuleNotConfigured_throwIllegalState() {
        assertThatThrownBy(() -> rateLimiter().tryAcquire("unknown", "x")).isInstanceOf(IllegalStateException.class);
    }

    @SuppressWarnings("unchecked")
    private void stubScript(List<Long> result) {
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of("ratelimit:log-in-email:hash")), eq("900000")))
                .thenReturn(result);
    }
}
