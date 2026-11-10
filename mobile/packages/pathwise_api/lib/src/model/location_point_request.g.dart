// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'location_point_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

LocationPointRequest _$LocationPointRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('LocationPointRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['lat', 'lon', 'recordedAt']);
  final val = LocationPointRequest(
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

Map<String, dynamic> _$LocationPointRequestToJson(
  LocationPointRequest instance,
) => <String, dynamic>{
  'accuracyMeters': ?instance.accuracyMeters,
  'headingDegrees': ?instance.headingDegrees,
  'lat': instance.lat,
  'lon': instance.lon,
  'recordedAt': instance.recordedAt.toIso8601String(),
  'speedMps': ?instance.speedMps,
};
