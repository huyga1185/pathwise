package uk.huy.pathwise.core.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller or a handler method whose writes stay allowed while the caller's store is READ_ONLY (T56, P-62).
 * Every other write (POST, PUT, PATCH, DELETE) of a store member is refused with {@code STORE_READ_ONLY} (423).
 * <p>
 * Only for writes that are not store data: the user's own account (profile, password, sessions) and what a driver
 * needs to finish the deliveries already running (stop results, location).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AllowedWhenStoreReadOnly {
}
