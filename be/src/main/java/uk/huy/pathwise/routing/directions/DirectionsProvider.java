package uk.huy.pathwise.routing.directions;

import uk.huy.pathwise.routing.model.Coordinate;

/**
 * Public API of the {@code routing} module for turn-by-turn directions (ARCHITECTURE.md §8.5, P-57). The bean is the
 * provider chosen by {@code routing.directions.provider}.
 */
public interface DirectionsProvider {
    /**
     * @throws uk.huy.pathwise.core.exception.ExternalServiceException {@code ROUTING_PROVIDER_ERROR} if the provider
     *         fails or times out, or finds no road between the points
     */
    Directions directions(Coordinate from, Coordinate to);
}
