package uk.huy.pathwise.store.dto.request;

/**
 * {@code PATCH /store/members/{userId}}: every field is optional and a missing one is left unchanged.
 */
public record UpdateMemberRequest(MemberStatusChange status, InvitableRole role, Boolean canDrive) {}
