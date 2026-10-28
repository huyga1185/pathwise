package uk.huy.pathwise.planning.dto.response;

import uk.huy.pathwise.planning.model.StopStatus;

import java.time.Instant;

/**
 * {@code DriverStop} of API_SPEC §8. The app builds its own Google or Apple Maps link from {@code lat}/{@code lon}
 * (D-07, P-32).
 *
 * @param recipientPhone E.164
 */
public record DriverStopResponse(long id,
                                 int sequence,
                                 String label,
                                 String recipientName,
                                 String recipientPhone,
                                 String address,
                                 double lat,
                                 double lon,
                                 Instant plannedArrival,
                                 TimeWindowResponse timeWindow,
                                 String note,
                                 StopStatus status,
                                 String failureReason) {}
