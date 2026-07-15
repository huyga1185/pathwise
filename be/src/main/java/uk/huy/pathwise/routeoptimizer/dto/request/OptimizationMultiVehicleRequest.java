package uk.huy.pathwise.routeoptimizer.dto.request;

import java.util.List;

public record OptimizationMultiVehicleRequest(List<String> addresses, int depot, int numberVehicle) {}
