package uk.huy.pathwise.support;

import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Answers geocoding from addresses registered by the test, so that no test calls Nominatim. An unknown address has
 * no result. Addresses are matched exactly, so each test can use its own.
 */
public class FakeGeocodingProvider implements GeocodingProvider {
    private final Map<String, GeocodingResult> results = new ConcurrentHashMap<>();
    private final Map<String, ErrorCode> failures = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> busyLeft = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> calls = new ConcurrentHashMap<>();
    private final Map<String, CountDownLatch> holds = new ConcurrentHashMap<>();

    @Override
    public List<GeocodingResult> search(String query, int limit) {
        calls.computeIfAbsent(query, key -> new AtomicInteger()).incrementAndGet();
        CountDownLatch hold = holds.get(query);
        if (hold != null) await(hold);
        AtomicInteger busy = busyLeft.get(query);
        if (busy != null && busy.getAndDecrement() > 0)
            throw new ExternalServiceException(ErrorCode.GEOCODING_BUSY, "Simulated busy rate limit", null);
        ErrorCode failure = failures.get(query);
        if (failure != null) throw new ExternalServiceException(failure, "Simulated provider failure", null);
        GeocodingResult result = results.get(query);
        return result == null ? List.of() : List.of(result);
    }

    public void resolve(String address, double lat, double lon) {
        failures.remove(address);
        results.put(address, new GeocodingResult(address + ", Việt Nam", lat, lon));
    }

    public void fail(String address) {
        failures.put(address, ErrorCode.GEOCODING_PROVIDER_ERROR);
    }

    /**
     * The next {@code times} searches of {@code address} find no free rate limit slot.
     */
    public void busy(String address, int times) {
        busyLeft.put(address, new AtomicInteger(times));
    }

    /**
     * Searches of {@code address} block until the returned latch is counted down, to keep a job RUNNING.
     */
    public CountDownLatch hold(String address) {
        CountDownLatch latch = new CountDownLatch(1);
        holds.put(address, latch);
        return latch;
    }

    public int calls(String address) {
        AtomicInteger count = calls.get(address);
        return count == null ? 0 : count.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Held geocoding never released");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
