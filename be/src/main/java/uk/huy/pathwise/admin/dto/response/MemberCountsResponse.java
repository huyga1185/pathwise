package uk.huy.pathwise.admin.dto.response;

/**
 * Members of a store by role, whatever their status.
 */
public record MemberCountsResponse(long owners, long dispatchers, long drivers) {}
