package uk.huy.pathwise.planning.service.location;

import uk.huy.pathwise.planning.model.LocationPoint;

import java.util.List;

/**
 * Where a driver's positions go (D-26, ARCHITECTURE.md §9). Every sink receives every batch, so keeping a history
 * later (I-05) is one more sink, without changing the API or the app.
 */
public interface LocationSink {
    /**
     * @param points the usable points of one batch, oldest first, never empty; points ahead of the server clock are
     *               already left out, old ones are not
     */
    void accept(long storeId, long driverId, List<LocationPoint> points);
}
