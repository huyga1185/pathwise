package uk.huy.pathwise.planning.service.location;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.planning.model.LocationPoint;
import uk.huy.pathwise.planning.store.DriverLocationStore;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * The MVP sink (P-15): keeps only the newest point of each driver, until it is {@link #MAX_AGE} old. A point older
 * than the stored one never replaces it, so a batch sent late after a network outage cannot move the driver back.
 */
@Component
@RequiredArgsConstructor
public class LastKnownLocationSink implements LocationSink {
    /** How long a position is shown: older, it says nothing useful about where the driver is. */
    public static final Duration MAX_AGE = Duration.ofMinutes(10);

    private final DriverLocationStore locationStore;

    @Override
    public void accept(long storeId, long driverId, List<LocationPoint> points) {
        LocationPoint newest = points.getLast();
        // Expires MAX_AGE after it was recorded, not after it arrived.
        Duration ttl = Duration.between(Instant.now(), newest.recordedAt().plus(MAX_AGE));
        if (ttl.isNegative() || ttl.isZero()) return;
        locationStore.saveIfNewer(storeId, driverId, newest, ttl);
    }
}
