import 'dart:math' as math;

import '../../shared/geo.dart';

/// What the driver does at the start of a step (API_SPEC §8, P-57).
enum Maneuver {
  depart,
  arrive,
  straight,
  slightLeft,
  slightRight,
  left,
  right,
  sharpLeft,
  sharpRight,
  uturn,
  keepLeft,
  keepRight,
  roundabout,
}

/// One instruction: [maneuver] happens at [location], then [distanceMeters] to the next step.
class DirectionsStep {
  const DirectionsStep({
    required this.maneuver,
    required this.distanceMeters,
    required this.durationSeconds,
    required this.location,
    this.exitNumber,
    this.roadName,
  });

  final Maneuver maneuver;
  final int? exitNumber;
  final String? roadName;
  final int distanceMeters;
  final int durationSeconds;
  final GeoPoint location;
}

/// A drive from where the driver was to [destination] (`GET /driver/routes/{routeId}/directions`).
class Directions {
  const Directions({
    required this.destination,
    required this.distanceMeters,
    required this.durationSeconds,
    required this.path,
    required this.steps,
    this.destinationStopId,
  });

  /// The stop driven to; null for the end of the route.
  final int? destinationStopId;
  final GeoPoint destination;
  final int distanceMeters;
  final int durationSeconds;
  final List<GeoPoint> path;
  final List<DirectionsStep> steps;
}

/// Where the driver is on the directions after a position (M05).
class Progress {
  const Progress({
    required this.nextStep,
    required this.metersToNextStep,
    required this.remainingMeters,
    required this.remainingSeconds,
    required this.offRoute,
    required this.arrived,
    required this.alongMeters,
  });

  /// Index in [Directions.steps] of the maneuver ahead (the last one, ARRIVE, at the end).
  final int nextStep;
  final double metersToNextStep;
  final double remainingMeters;
  final int remainingSeconds;

  /// This position is too far from the path. One such position proves nothing (GPS jumps); the screen reroutes after
  /// several in a row ([Guidance.offRouteFixes]).
  final bool offRoute;

  /// Close enough to the destination to report the result.
  final bool arrived;

  /// Distance travelled along the path, to keep progress from going backwards.
  final double alongMeters;
}

/// Follows a driver along [directions]: the next maneuver, what is left, off the route or arrived. Pure: the screen
/// feeds it positions (ARCHITECTURE §2: logic outside widgets).
class Guidance {
  Guidance(this.directions)
    : path = PolylinePath(directions.path.isEmpty ? [directions.destination] : directions.path) {
    var notBefore = 0.0;
    for (final step in directions.steps) {
      final along = path.project(step.location, notBeforeMeters: notBefore).alongMeters;
      _stepAlong.add(along);
      notBefore = along;
    }
  }

  /// Off the route when farther than this from the path, or than the position's accuracy if worse.
  static const offRouteMeters = 50.0;

  /// Positions in a row off the route before asking for new directions.
  static const offRouteFixes = 3;

  /// Positions less accurate than this say nothing about being off the route.
  static const unusableAccuracyMeters = 100.0;

  /// Arrived within this straight-line distance of the destination.
  static const arrivedMeters = 40.0;

  /// A maneuver this close behind the driver counts as passed.
  static const passedMeters = 10.0;

  final Directions directions;
  final PolylinePath path;
  final _stepAlong = <double>[];

  /// The progress before any position: at the start, the first maneuver after DEPART ahead.
  Progress get start => _progress(along: 0, offRoute: false, arrived: false);

  /// [previous] keeps progress from going backwards where the path comes back near itself.
  Progress update(GeoPoint position, {double? accuracyMeters, Progress? previous}) {
    final projection = path.project(position, notBeforeMeters: math.max(0, (previous?.alongMeters ?? 0) - 30));
    final usable = accuracyMeters == null || accuracyMeters <= unusableAccuracyMeters;
    final threshold = math.max(offRouteMeters, accuracyMeters ?? 0);
    final arrived = distanceMeters(position, directions.destination) <= arrivedMeters;
    return _progress(
      along: projection.alongMeters,
      offRoute: usable && !arrived && projection.offsetMeters > threshold,
      arrived: arrived || (previous?.arrived ?? false),
    );
  }

  Progress _progress({required double along, required bool offRoute, required bool arrived}) {
    final steps = directions.steps;
    var next = steps.length - 1;
    for (var i = 1; i < steps.length; i++) {
      if (_stepAlong[i] > along + passedMeters) {
        next = i;
        break;
      }
    }
    final total = path.lengthMeters;
    final remaining = math.max(0.0, total - along);
    final seconds = total <= 0 ? 0 : (directions.durationSeconds * remaining / total).round();
    return Progress(
      nextStep: next < 0 ? 0 : next,
      metersToNextStep: next < 0 ? 0 : math.max(0, _stepAlong[next] - along),
      remainingMeters: remaining,
      remainingSeconds: seconds,
      offRoute: offRoute,
      arrived: arrived,
      alongMeters: along,
    );
  }
}
