package uk.huy.pathwise.store.dto.response;

import org.jspecify.annotations.Nullable;
public record DepotResponse(@Nullable String address, double lat, double lon) {}
