package uk.huy.pathwise.audit.dto;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.audit.model.AuditAction;

import java.time.Instant;

/**
 * Filters of an audit search; null means "any". {@code from} inclusive, {@code to} exclusive.
 */
public record AuditFilter(@Nullable Long storeId,
                          @Nullable Long actorId,
                          @Nullable AuditAction action,
                          @Nullable Instant from,
                          @Nullable Instant to) {}
