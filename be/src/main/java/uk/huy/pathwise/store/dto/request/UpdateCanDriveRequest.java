package uk.huy.pathwise.store.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateCanDriveRequest(@NotNull(message = "CAN_DRIVE_REQUIRED") Boolean canDrive) {}
