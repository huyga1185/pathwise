package uk.huy.pathwise.routing.infrastructure.cache;

import uk.huy.pathwise.core.statestore.StateStore;

/**
 * Geocoding results as JSON, keyed by provider and query (DATA_MODEL.md §3).
 */
public interface GeocodingCacheStore extends StateStore<String, String> {}
