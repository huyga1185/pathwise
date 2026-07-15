package uk.huy.pathwise.routeoptimizer.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class OptimizationRequest {
    private List<String> addresses;
    private int depot;
}
