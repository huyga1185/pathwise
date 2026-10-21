package uk.huy.pathwise.core.statestore;

import java.time.Duration;
import java.util.Optional;

/**
 * A generic key-value store designed for managing temporary state with expiration times.
 * <p>
 * <b>Design Note:</b> This interface intentionally omits a {@code save} method without a
 * Time-To-Live (TTL) parameter. This is a deliberate architectural decision to enforce best
 * practices, as the vast majority of temporary state management tasks should strictly define
 * an expiration. If indefinite storage or a default TTL is required, developers should implement
 * that custom logic within their specific concrete classes.
 * </p>
 *
 * @param <K> the type of keys maintained by this store
 * @param <V> the type of mapped values
 */
public interface StateStore<K, V> {
    /**
     * Saves or updates a key-value pair in the store with a specific Time-To-Live (TTL).
     * If the key already exists, its value and TTL will be overwritten.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @param ttl   the duration after which the entry should expire and be automatically removed
     */
    void save(K key, V value, Duration ttl);
    /**
     * Retrieves the value associated with the specified key.
     *
     * @param key the key whose associated value is to be returned
     * @return an {@link Optional} containing the value if it exists and has not expired,
     *         or {@link Optional#empty()} if the key does not exist or has expired
     */
    Optional<V> find(K key);
    /**
     * Removes the key and its associated value from the store.
     * If the key does not exist in the store, this operation should have no effect (idempotent).
     *
     * @param key the key to be removed from the store
     */
    void delete(K key);
}
