import 'dart:async';

import 'package:pathwise_driver/data/api_error.dart';
import 'package:pathwise_driver/features/navigation/directions_source.dart';
import 'package:pathwise_driver/features/navigation/guidance.dart';
import 'package:pathwise_driver/platform/screen_awake.dart';
import 'package:pathwise_driver/shared/geo.dart';

/// Directions in memory: [next] answers the following requests (or [error] fails them); [requests] records them.
class FakeDirectionsSource implements DirectionsSource {
  FakeDirectionsSource({this.next});

  Directions? next;
  ApiError? error;
  final requests = <({int routeId, GeoPoint from, int? toStopId})>[];

  /// Holds requests until completed, to see the screen while they run.
  Completer<void>? hold;

  @override
  Future<Directions> directions({required int routeId, required GeoPoint from, int? toStopId}) async {
    requests.add((routeId: routeId, from: from, toStopId: toStopId));
    await hold?.future;
    if (error case final error?) throw error;
    return next ?? straightDirections(from, const GeoPoint(16.048, 108.196), stopId: toStopId);
  }
}

/// A drive due east along one street: depart, turn left after [legMeters], arrive.
Directions straightDirections(GeoPoint from, GeoPoint to, {int? stopId}) {
  final corner = GeoPoint(from.lat, to.lon);
  return Directions(
    destinationStopId: stopId,
    destination: to,
    distanceMeters: 1000,
    durationSeconds: 180,
    path: [from, corner, to],
    steps: [
      DirectionsStep(
        maneuver: Maneuver.depart,
        roadName: 'Lê Duẩn',
        distanceMeters: 500,
        durationSeconds: 90,
        location: from,
      ),
      DirectionsStep(
        maneuver: Maneuver.left,
        roadName: 'Nguyễn Văn Linh',
        distanceMeters: 500,
        durationSeconds: 90,
        location: corner,
      ),
      DirectionsStep(maneuver: Maneuver.arrive, distanceMeters: 0, durationSeconds: 0, location: to),
    ],
  );
}

class FakeScreenAwake implements ScreenAwake {
  bool on = false;

  @override
  Future<void> keepOn() async => on = true;

  @override
  Future<void> release() async => on = false;
}
