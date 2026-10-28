package uk.huy.pathwise.store.dto;

/**
 * Store settings as other modules see them (ARCHITECTURE.md §2.1), for example the default depot of a plan (D-20).
 *
 * @param depotLat null when the store has no depot; set together with {@code depotLon}
 * @param timezone IANA zone id; time windows and shifts are read in this zone (P-14)
 */
public record StoreProfile(long id,
                           String name,
                           String depotAddress,
                           Double depotLat,
                           Double depotLon,
                           String timezone) {}
