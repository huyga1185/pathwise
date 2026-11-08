package uk.huy.pathwise.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Product and company identity shown in every email ({@code service.identity.*}).
 */
@ConfigurationProperties("service.identity")
public record ServiceIdentityProperties(String appName,
                                        String appUrl,
                                        String companyName,
                                        String companyUrl,
                                        String companyAddress) {}
