package uk.huy.pathwise.user.dto.request;

import uk.huy.pathwise.user.infrastructure.phonenumber.ValidPhoneNumber;

public record UserModificationRequest(@ValidPhoneNumber String phoneNumber) {}
