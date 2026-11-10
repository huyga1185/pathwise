// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_position.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverPosition _$DriverPositionFromJson(Map<String, dynamic> json) =>
    $checkedCreate('DriverPosition', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'accuracyMeters',
          'headingDegrees',
          'lat',
          'lon',
          'recordedAt',
          'speedMps',
        ],
      );
      final val = DriverPosition(
        accuracyMeters: $checkedConvert(
          'accuracyMeters',
          (v) => (v as num?)?.toDouble(),
        ),
        headingDegrees: $checkedConvert(
          'headingDegrees',
          (v) => (v as num?)?.toDouble(),
        ),
        lat: $checkedConvert('lat', (v) => (v as num).toDouble()),
        lon: $checkedConvert('lon', (v) => (v as num).toDouble()),
        recordedAt: $checkedConvert(
          'recordedAt',
          (v) => DateTime.parse(v as String),
        ),
        speedMps: $checkedConvert('speedMps', (v) => (v as num?)?.toDouble()),
      );
      return val;
    });

Map<String, dynamic> _$DriverPositionToJson(DriverPosition instance) =>
    <String, dynamic>{
      'accuracyMeters': instance.accuracyMeters,
      'headingDegrees': instance.headingDegrees,
      'lat': instance.lat,
      'lon': instance.lon,
      'recordedAt': instance.recordedAt.toIso8601String(),
      'speedMps': instance.speedMps,
    };
