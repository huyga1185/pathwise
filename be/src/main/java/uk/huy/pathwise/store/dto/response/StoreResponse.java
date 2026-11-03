package uk.huy.pathwise.store.dto.response;

import org.jspecify.annotations.Nullable;
/**
 * @param depot null when the store has no depot
 */
public record StoreResponse(long id,
                            String name,
                            @Nullable DepotResponse depot,
                            String timezone) {}
