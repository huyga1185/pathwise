package uk.huy.pathwise.job.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import uk.huy.pathwise.job.model.JobType;

/**
 * {@code jobs.*}: the mode of each job type and the async thread pool (ARCHITECTURE.md §8.4). Every type must have a
 * mode in configuration; there is no default, so that moving optimize to async stays a deliberate change.
 */
@ConfigurationProperties("jobs")
public record JobProperties(TypeSettings geocodePlan,
                            TypeSettings optimizePlan,
                            @DefaultValue Executor executor) {
    public record TypeSettings(JobMode mode) {}

    public record Executor(@DefaultValue("2") int poolSize, @DefaultValue("50") int queueCapacity) {}

    /**
     * @throws IllegalStateException if the type has no {@code jobs.<type>.mode}
     */
    public JobMode modeOf(JobType type) {
        TypeSettings settings = switch (type) {
            case GEOCODE_PLAN -> geocodePlan;
            case OPTIMIZE_PLAN -> optimizePlan;
        };
        if (settings == null || settings.mode() == null)
            throw new IllegalStateException("Missing jobs.<type>.mode for job type " + type);
        return settings.mode();
    }
}
