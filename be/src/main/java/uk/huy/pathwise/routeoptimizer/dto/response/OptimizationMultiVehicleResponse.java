package uk.huy.pathwise.routeoptimizer.dto.response;

import java.util.List;

public record OptimizationMultiVehicleResponse(List<List<String>> optimizedRoute, int depot) {}
