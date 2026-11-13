import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:pathwise_api/pathwise_api.dart' as api;

import '../../app/providers.dart';
import '../../data/api_error.dart';
import '../../shared/geo.dart';
import 'guidance.dart';

/// Directions port (ARCHITECTURE §2.1, T33, P-57): a drive from [from] to a stop of the route, the first stop not yet
/// done if [toStopId] is null. Throws an [ApiError]. Tests use a fake.
abstract interface class DirectionsSource {
  Future<Directions> directions({required int routeId, required GeoPoint from, int? toStopId});
}

/// `GET /driver/routes/{routeId}/directions`, mapped to the app's types.
class ApiDirectionsSource implements DirectionsSource {
  const ApiDirectionsSource(this._ref);

  final Ref _ref;

  @override
  Future<Directions> directions({required int routeId, required GeoPoint from, int? toStopId}) async {
    final routes = _ref.read(apiClientProvider).api.getDriverRouteControllerApi();
    final response = await guard(
      () => routes.getDirections(routeId: routeId, lat: from.lat, lon: from.lon, toStopId: toStopId),
    );
    return directionsOf(response.data!.data);
  }
}

/// The API's directions as the app's. Throws [FormatException] on a polyline it cannot read.
Directions directionsOf(api.DirectionsResponse response) => Directions(
  destinationStopId: response.destination.stopId,
  destination: GeoPoint(response.destination.lat, response.destination.lon),
  distanceMeters: response.distanceMeters,
  durationSeconds: response.durationSeconds,
  path: decodePolyline6(response.polyline),
  steps: [
    for (final step in response.steps)
      DirectionsStep(
        maneuver: _maneuvers[step.maneuver]!,
        exitNumber: step.exitNumber,
        roadName: step.roadName,
        distanceMeters: step.distanceMeters,
        durationSeconds: step.durationSeconds,
        location: GeoPoint(step.location.lat, step.location.lon),
      ),
  ],
);

const _maneuvers = {
  api.DirectionsInstructionManeuverEnum.DEPART: Maneuver.depart,
  api.DirectionsInstructionManeuverEnum.ARRIVE: Maneuver.arrive,
  api.DirectionsInstructionManeuverEnum.STRAIGHT: Maneuver.straight,
  api.DirectionsInstructionManeuverEnum.SLIGHT_LEFT: Maneuver.slightLeft,
  api.DirectionsInstructionManeuverEnum.SLIGHT_RIGHT: Maneuver.slightRight,
  api.DirectionsInstructionManeuverEnum.LEFT: Maneuver.left,
  api.DirectionsInstructionManeuverEnum.RIGHT: Maneuver.right,
  api.DirectionsInstructionManeuverEnum.SHARP_LEFT: Maneuver.sharpLeft,
  api.DirectionsInstructionManeuverEnum.SHARP_RIGHT: Maneuver.sharpRight,
  api.DirectionsInstructionManeuverEnum.UTURN: Maneuver.uturn,
  api.DirectionsInstructionManeuverEnum.KEEP_LEFT: Maneuver.keepLeft,
  api.DirectionsInstructionManeuverEnum.KEEP_RIGHT: Maneuver.keepRight,
  api.DirectionsInstructionManeuverEnum.ROUNDABOUT: Maneuver.roundabout,
};

final directionsSourceProvider = Provider<DirectionsSource>(ApiDirectionsSource.new);
