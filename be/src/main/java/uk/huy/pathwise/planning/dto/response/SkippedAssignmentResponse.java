package uk.huy.pathwise.planning.dto.response;

/**
 * An assignment of the copied plan that was left out of the copy (T62).
 */
public record SkippedAssignmentResponse(long driverId, long vehicleId, Reason reason) {
    public enum Reason {
        /** The driver is no longer an ACTIVE member who may drive. */
        DRIVER_UNAVAILABLE,
        /** The vehicle is deactivated. */
        VEHICLE_INACTIVE
    }
}
