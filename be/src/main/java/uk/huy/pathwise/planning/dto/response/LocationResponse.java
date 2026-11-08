package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
public record LocationResponse(@Nullable String address, double lat, double lon) {}
