package uk.huy.pathwise.user.dto.request;

import uk.huy.pathwise.shared.phonenumber.ValidPhoneNumber;

public record UserModificationRequest(@ValidPhoneNumber String phoneNumber) {}
