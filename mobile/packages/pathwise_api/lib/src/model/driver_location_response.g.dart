// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_location_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverLocationResponse _$DriverLocationResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DriverLocationResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const ['driverId', 'location', 'routeId', 'sharing'],
  );
  final val = DriverLocationResponse(
    driverId: $checkedConvert('driverId', (v) => (v as num).toInt()),
    location: $checkedConvert(
      'location',
      (v) =>
          v == null ? null : DriverPosition.fromJson(v as Map<String, dynamic>),
    ),
    routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
    sharing: $checkedConvert(
      'sharing',
      (v) =>
          v == null ? null : DriverSharing.fromJson(v as Map<String, dynamic>),
    ),
  );
  return val;
});

Map<String, dynamic> _$DriverLocationResponseToJson(
  DriverLocationResponse instance,
) => <String, dynamic>{
  'driverId': instance.driverId,
  'location': instance.location?.toJson(),
  'routeId': instance.routeId,
  'sharing': instance.sharing?.toJson(),
};
