package uk.huy.pathwise.store.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Replaces the store settings ({@code PUT /store}).
 *
 * @param depot null for no depot; plans then need a depot of their own (422 {@code STORE_DEPOT_REQUIRED})
 */
public record UpdateStoreRequest(@NotBlank(message = "STORE_NAME_REQUIRED") @Size(max = 150, message = "INVALID_STORE_NAME_LENGTH") String name,
                                 @Valid DepotRequest depot,
                                 @NotBlank(message = "TIMEZONE_REQUIRED") @Size(max = 50, message = "INVALID_TIMEZONE") String timezone) {}
