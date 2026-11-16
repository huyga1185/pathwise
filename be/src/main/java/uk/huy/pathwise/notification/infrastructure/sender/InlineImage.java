package uk.huy.pathwise.notification.infrastructure.sender;

import org.springframework.core.io.Resource;

/**
 * An image carried inside the email and shown by the HTML as {@code <img src="cid:contentId">} (P-70): it shows
 * without the client loading anything from the internet.
 *
 * @param contentId   referenced by the HTML after {@code cid:}
 * @param resource    the image's bytes
 * @param contentType MIME type, e.g. {@code image/png}
 */
public record InlineImage(String contentId, Resource resource, String contentType) {}
