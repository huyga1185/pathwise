// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'directions_destination.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DirectionsDestination _$DirectionsDestinationFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DirectionsDestination', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['lat', 'lon', 'stopId']);
  final val = DirectionsDestination(
    lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
    lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
    stopId: $checkedConvert('stopId', (v) => (v as num?)?.toInt()),
  );
  return val;
});

Map<String, dynamic> _$DirectionsDestinationToJson(
  DirectionsDestination instance,
) => <String, dynamic>{
  'lat': instance.lat,
  'lon': instance.lon,
  'stopId': instance.stopId,
};
