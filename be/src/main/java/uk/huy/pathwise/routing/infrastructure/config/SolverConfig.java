package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.routing.infrastructure.ortools.OrToolsVrpSolver;
import uk.huy.pathwise.routing.solver.SolverProvider;

/**
 * The single {@link SolverProvider} bean, chosen by {@code routing.solver.provider} (ARCHITECTURE.md §8.5).
 */
@Configuration
@EnableConfigurationProperties(SolverProperties.class)
public class SolverConfig {
    @Bean
    SolverProvider vrpSolverProvider(SolverProperties properties) {
        return switch (properties.provider()) {
            case "ortools" -> new OrToolsVrpSolver(properties.timeLimit());
            default -> throw new IllegalStateException("Unknown routing.solver.provider: " + properties.provider());
        };
    }
}
