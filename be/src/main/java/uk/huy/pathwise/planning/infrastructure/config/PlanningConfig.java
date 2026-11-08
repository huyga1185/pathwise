package uk.huy.pathwise.planning.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeocodePlanProperties.class)
class PlanningConfig {}
