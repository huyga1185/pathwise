package uk.huy.pathwise.store.dto.response;

/**
 * @param depot null when the store has no depot
 */
public record StoreResponse(long id,
                            String name,
                            DepotResponse depot,
                            String timezone) {}
