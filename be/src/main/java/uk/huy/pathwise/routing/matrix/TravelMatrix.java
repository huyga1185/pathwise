package uk.huy.pathwise.routing.matrix;

/**
 * Travel distance and time between every ordered pair of points, in the order the points were given:
 * {@code distanceMeters[i][j]} is from point i to point j. Whole metres and seconds (P-14).
 */
public record TravelMatrix(int[][] distanceMeters, int[][] durationSeconds) {
    public int size() {
        return distanceMeters.length;
    }
}
