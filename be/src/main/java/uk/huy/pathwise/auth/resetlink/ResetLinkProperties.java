package uk.huy.pathwise.auth.resetlink;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * {@code auth.reset-link.*} (T57).
 *
 * @param ttl  how long a link sent by an ADMIN can be opened (24 hours, P-63)
 * @param path page of the web app that opens it, after {@code service-identity.app-url}
 */
@ConfigurationProperties("auth.reset-link")
public record ResetLinkProperties(@DefaultValue("24h") Duration ttl,
                                  @DefaultValue("/app/reset-password") String path) {}
