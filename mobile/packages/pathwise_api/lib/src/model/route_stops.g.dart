// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'route_stops.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

RouteStops _$RouteStopsFromJson(Map<String, dynamic> json) =>
    $checkedCreate('RouteStops', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['routeId', 'stopIds']);
      final val = RouteStops(
        routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
        stopIds: $checkedConvert(
          'stopIds',
          (v) => (v as List<dynamic>).map((e) => (e as num).toInt()).toList(),
        ),
      );
      return val;
    });

Map<String, dynamic> _$RouteStopsToJson(RouteStops instance) =>
    <String, dynamic>{'routeId': instance.routeId, 'stopIds': instance.stopIds};
