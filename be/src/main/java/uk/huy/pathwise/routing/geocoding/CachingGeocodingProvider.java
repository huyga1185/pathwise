package uk.huy.pathwise.routing.geocoding;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.routing.infrastructure.cache.GeocodingCacheStore;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Caches search results in Redis (P-11), in front of the real provider, so a cache hit uses no rate limit slot and no
 * provider call. Empty results are cached too; failures are not.
 * <p>
 * Key: {@code geocode:{provider}:{sha256(normalized query + "|" + limit)}}. The query is normalized by trimming,
 * collapsing whitespace and lower-casing, so trivial variants of an address share one entry.
 */
public class CachingGeocodingProvider implements GeocodingProvider {
    private static final TypeReference<List<GeocodingResult>> RESULTS = new TypeReference<>() {};

    private final GeocodingProvider delegate;
    private final String providerName;
    private final GeocodingCacheStore cache;
    private final Hasher hasher;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public CachingGeocodingProvider(GeocodingProvider delegate,
                                    String providerName,
                                    GeocodingCacheStore cache,
                                    Hasher hasher,
                                    ObjectMapper objectMapper,
                                    Duration ttl) {
        this.delegate = delegate;
        this.providerName = providerName;
        this.cache = cache;
        this.hasher = hasher;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public List<GeocodingResult> search(String query, int limit) {
        String normalized = normalize(query);
        String key = "geocode:" + providerName + ":" + hasher.hashString(normalized + "|" + limit);
        return cache.find(key)
                .map(json -> objectMapper.readValue(json, RESULTS))
                .orElseGet(() -> {
                    List<GeocodingResult> results = delegate.search(normalized, limit);
                    cache.save(key, objectMapper.writeValueAsString(results), ttl);
                    return results;
                });
    }

    static String normalize(String query) {
        if (query == null) throw new NullPointerException("Query could not be null");
        return query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
