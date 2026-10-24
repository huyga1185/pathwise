package uk.huy.pathwise.auth.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code auth.cookie.*}: attributes of auth cookies.
 *
 * @param sameSite SameSite attribute (D-22, ARCHITECTURE.md §6.4); env {@code AUTH_COOKIE_SAME_SITE}
 */
@ConfigurationProperties("auth.cookie")
public record AuthCookieProperties(@DefaultValue("Strict") String sameSite) {}
