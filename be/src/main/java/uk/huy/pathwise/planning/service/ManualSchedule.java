package uk.huy.pathwise.planning.service;

import java.util.ArrayList;
import java.util.List;

/**
 * The timetable of a route edited by hand (T28, P-58), without the solver: the driver leaves the start at the beginning
 * of the shift, or later so as not to wait at the first stop; arriving early at a stop they wait for its time window to
 * open; service takes its time; then on to the next leg. Times are seconds from 00:00 of the plan date (P-14).
 */
final class ManualSchedule {
    private ManualSchedule() {}

    /**
     * @param serviceStarts when service begins at each stop: the planned arrival of API_SPEC §7
     * @param startSeconds  when the driver leaves the start point
     * @param endSeconds    when the driver is back at the end point
     */
    record RouteTimes(int startSeconds, int endSeconds, int distanceMeters, List<Integer> serviceStarts) {
        int durationSeconds() {
            return endSeconds - startSeconds;
        }
    }

    /**
     * One stop of the route, in visiting order.
     *
     * @param windowStart null without a time window
     */
    record Visit(int serviceSeconds, Integer windowStart) {}

    /**
     * @param legDurations seconds of each leg: start → first stop, …, last stop → end ({@code visits.size() + 1})
     * @param legDistances metres of the same legs
     */
    static RouteTimes compute(int shiftStart, List<Visit> visits, int[] legDurations, int[] legDistances) {
        if (visits.isEmpty()) throw new IllegalArgumentException("A route with stops is required");
        if (legDurations.length != visits.size() + 1 || legDistances.length != visits.size() + 1)
            throw new IllegalArgumentException("One leg more than visits is required");
        Integer firstWindow = visits.getFirst().windowStart();
        int start = firstWindow == null ? shiftStart : Math.max(shiftStart, firstWindow - legDurations[0]);
        int time = start;
        List<Integer> serviceStarts = new ArrayList<>(visits.size());
        for (int i = 0; i < visits.size(); i++) {
            Visit visit = visits.get(i);
            int arrival = time + legDurations[i];
            int serviceStart = visit.windowStart() == null ? arrival : Math.max(arrival, visit.windowStart());
            serviceStarts.add(serviceStart);
            time = serviceStart + visit.serviceSeconds();
        }
        int end = time + legDurations[visits.size()];
        int distance = 0;
        for (int meters : legDistances) distance += meters;
        return new RouteTimes(start, end, distance, List.copyOf(serviceStarts));
    }
}
