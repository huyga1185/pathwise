// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'directions_point.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DirectionsPoint _$DirectionsPointFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DirectionsPoint', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['lat', 'lon']);
      final val = DirectionsPoint(
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$DirectionsPointToJson(DirectionsPoint instance) =>
    <String, dynamic>{'lat': instance.lat, 'lon': instance.lon};
