package uk.huy.pathwise.routing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.routing.geocoding.CachingGeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;
import uk.huy.pathwise.routing.infrastructure.cache.GeocodingCacheStore;
import uk.huy.pathwise.routing.infrastructure.nominatim.NominatimGeocodingProvider;
import uk.huy.pathwise.routing.infrastructure.throttle.RedisRequestThrottle;
import uk.huy.pathwise.routing.infrastructure.throttle.RequestThrottle;
import uk.huy.pathwise.support.AbstractIntegrationTest;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * T21 acceptance against a real Redis: cache in front of Nominatim, shared rate limit (P-11, D-25).
 */
class GeocodingCacheAndThrottleIntegrationTest extends AbstractIntegrationTest {
    private static final String ONE_RESULT = "[{\"display_name\":\"Đà Nẵng\",\"lat\":\"16.05\",\"lon\":\"108.2\"}]";

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private GeocodingCacheStore cache;

    @Autowired
    private Hasher hasher;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void search_whenSameQueryTwice_secondComesFromCacheWithoutHttpCall() {
        Setup setup = setup(() -> true);
        String query = "Cầu Rồng " + UUID.randomUUID();
        setup.server().expect(once(), method(HttpMethod.GET)).andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        GeocodingResult first = setup.provider().search(query, 1).getFirst();
        // Same query after normalization (case, spaces).
        GeocodingResult second = setup.provider().search("  " + query.toUpperCase() + "  ", 1).getFirst();

        assertThat(second).isEqualTo(first).isEqualTo(new GeocodingResult("Đà Nẵng", 16.05, 108.2));
        setup.server().verify();
        String key = "geocode:test:" + hasher.hashString(query.toLowerCase() + "|1");
        assertThat(redis.getExpire(key)).isBetween(Duration.ofHours(24).toSeconds() - 5, Duration.ofHours(24).toSeconds());
    }

    @Test
    void search_whenProviderFails_errorIsNotCached() {
        Setup setup = setup(() -> true);
        String query = "fail " + UUID.randomUUID();
        setup.server().expect(once(), method(HttpMethod.GET)).andRespond(withServerError());
        setup.server().expect(once(), method(HttpMethod.GET)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> setup.provider().search(query, 1)).isInstanceOf(ExternalServiceException.class);
        assertThat(setup.provider().search(query, 1)).isEmpty();
        // An empty result is cached: no third call.
        assertThat(setup.provider().search(query, 1)).isEmpty();
        setup.server().verify();
    }

    @Test
    void search_whenRateLimitDisabled_consecutiveCallsDoNotWait() {
        Setup setup = setup(() -> true);
        setup.server().expect(times(5), method(HttpMethod.GET)).andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        long start = System.nanoTime();
        for (int i = 0; i < 5; i++) setup.provider().search("free " + UUID.randomUUID(), 1);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(800));
        setup.server().verify();
    }

    @Test
    void search_whenRateLimitedAndSlotTaken_waitForNextSlot() {
        Setup setup = setup(throttle(1, Duration.ofSeconds(2)));
        setup.server().expect(times(2), method(HttpMethod.GET)).andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        setup.provider().search("a " + UUID.randomUUID(), 1);
        long start = System.nanoTime();
        setup.provider().search("b " + UUID.randomUUID(), 1);

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThan(Duration.ofMillis(700));
    }

    @Test
    void search_whenNoSlotWithinMaxWait_throwGeocodingBusy() {
        Setup setup = setup(throttle(1, Duration.ofMillis(200)));
        setup.server().expect(once(), method(HttpMethod.GET)).andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        setup.provider().search("a " + UUID.randomUUID(), 1);

        assertThatThrownBy(() -> setup.provider().search("b " + UUID.randomUUID(), 1))
                .isInstanceOf(ExternalServiceException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.GEOCODING_BUSY);
    }

    @Test
    void acquire_whenCallersArriveTogether_spaceThemOneIntervalApartInArrivalOrder() throws Exception {
        String key = "geocode:test:next-slot:" + UUID.randomUUID();
        RequestThrottle throttle = new RedisRequestThrottle(redis, key, 10, Duration.ofSeconds(2));
        int callers = 5;
        List<Long> grantedAt = Collections.synchronizedList(new ArrayList<>());
        try (ExecutorService pool = Executors.newFixedThreadPool(callers)) {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int i = 0; i < callers; i++)
                results.add(pool.submit(() -> {
                    boolean acquired = throttle.acquire();
                    grantedAt.add(System.nanoTime());
                    return acquired;
                }));
            for (Future<Boolean> result : results) assertThat(result.get()).isTrue();
        }

        List<Long> sorted = grantedAt.stream().sorted().toList();
        // 10 per second: one slot every 100 ms, nobody shares a slot.
        for (int i = 1; i < sorted.size(); i++)
            assertThat(Duration.ofNanos(sorted.get(i) - sorted.get(i - 1))).isGreaterThan(Duration.ofMillis(70));
        assertThat(redis.getExpire(key, TimeUnit.MILLISECONDS)).isPositive().isLessThanOrEqualTo(100L);
    }

    @Test
    void acquire_whenRejected_reserveNothingSoTheQueueDoesNotGrow() {
        String key = "geocode:test:next-slot:" + UUID.randomUUID();
        RequestThrottle patient = new RedisRequestThrottle(redis, key, 2, Duration.ofSeconds(2));
        RequestThrottle impatient = new RedisRequestThrottle(redis, key, 2, Duration.ZERO);

        assertThat(patient.acquire()).isTrue();
        for (int i = 0; i < 5; i++) assertThat(impatient.acquire()).isFalse();

        // Only the first slot was taken: the next caller waits one interval (500 ms), not six.
        long start = System.nanoTime();
        assertThat(patient.acquire()).isTrue();
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(800));
    }

    @Test
    void search_whenCacheHit_useNoRateLimitSlot() {
        Setup setup = setup(throttle(1, Duration.ZERO));
        String query = "hit " + UUID.randomUUID();
        setup.server().expect(once(), method(HttpMethod.GET)).andRespond(withSuccess(ONE_RESULT, MediaType.APPLICATION_JSON));

        setup.provider().search(query, 1);

        // The slot of the first call is still taken, yet the cached query answers at once.
        assertThat(setup.provider().search(query, 1)).hasSize(1);
    }

    private RequestThrottle throttle(int perSecond, Duration maxWait) {
        return new RedisRequestThrottle(redis, "geocode:test:next-slot:" + UUID.randomUUID(), perSecond, maxWait);
    }

    private Setup setup(RequestThrottle throttle) {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://nominatim.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(false).build();
        GeocodingProvider provider = new CachingGeocodingProvider(new NominatimGeocodingProvider(builder.build(), throttle),
                "test", cache, hasher, objectMapper, Duration.ofHours(24));
        return new Setup(server, provider);
    }

    private record Setup(MockRestServiceServer server, GeocodingProvider provider) {}
}
