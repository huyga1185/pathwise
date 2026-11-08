package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;
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
                                 @Nullable String recipientName,
                                 @Nullable String recipientPhone,
                                 @Nullable String address,
                                 double lat,
                                 double lon,
                                 @Nullable Instant plannedArrival,
                                 @Nullable TimeWindowResponse timeWindow,
                                 @Nullable String note,
                                 StopStatus status,
                                 @Nullable String failureReason) {}
