package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code routing.solver.*} (ARCHITECTURE.md §8.3, §8.5).
 *
 * @param provider  implementation to use; only {@code ortools}
 * @param timeLimit how long one optimization searches; with guided local search every solve takes this long
 */
@ConfigurationProperties(prefix = "routing.solver")
public record SolverProperties(@DefaultValue("ortools") String provider,
                               @DefaultValue("10s") Duration timeLimit) {}
