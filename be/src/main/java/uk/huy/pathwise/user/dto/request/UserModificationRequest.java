package uk.huy.pathwise.user.dto.request;

import uk.huy.pathwise.core.phonenumber.ValidPhoneNumber;

public record UserModificationRequest(@ValidPhoneNumber String phoneNumber) {}
