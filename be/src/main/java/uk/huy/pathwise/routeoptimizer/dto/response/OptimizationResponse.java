package uk.huy.pathwise.routeoptimizer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Builder
@Getter
public class OptimizationResponse {
    private List<String> optimizedRoute;
    int depot;
}
