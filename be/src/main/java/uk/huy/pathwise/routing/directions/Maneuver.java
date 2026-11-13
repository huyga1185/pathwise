package uk.huy.pathwise.routing.directions;

/**
 * What the driver does at the start of a step (API_SPEC §8, P-57). PathWise's own vocabulary: providers map theirs
 * onto it, and the app writes the instruction text. Adding a value breaks apps built before it.
 */
public enum Maneuver {
    DEPART,
    ARRIVE,
    STRAIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    LEFT,
    RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    UTURN,
    /** Take the left branch: a fork, a merge, a ramp. */
    KEEP_LEFT,
    /** Take the right branch: a fork, a merge, a ramp. */
    KEEP_RIGHT,
    /** Enter a roundabout; the step's exit number says which exit to take. */
    ROUNDABOUT
}
