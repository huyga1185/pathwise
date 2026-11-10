package uk.huy.pathwise.planning.model;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * One position reported by a driver's phone (D-26). Coordinates in degrees, accuracy in metres, heading in degrees
 * clockwise from north, speed in metres per second.
 */
public record LocationPoint(double lat,
                            double lon,
                            @Nullable Double accuracyMeters,
                            @Nullable Double headingDegrees,
                            @Nullable Double speedMps,
                            Instant recordedAt) {}
