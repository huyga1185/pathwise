// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_route_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverRouteDetailResponse _$DriverRouteDetailResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DriverRouteDetailResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'end',
      'planDate',
      'planId',
      'planName',
      'routeId',
      'start',
      'status',
      'stops',
      'timezone',
    ],
  );
  final val = DriverRouteDetailResponse(
    end: $checkedConvert(
      'end',
      (v) => LocationResponse.fromJson(v as Map<String, dynamic>),
    ),
    planDate: $checkedConvert('planDate', (v) => DateTime.parse(v as String)),
    planId: $checkedConvert('planId', (v) => (v as num).toInt()),
    planName: $checkedConvert('planName', (v) => v as String),
    routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
    start: $checkedConvert(
      'start',
      (v) => LocationResponse.fromJson(v as Map<String, dynamic>),
    ),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$DriverRouteDetailResponseStatusEnumEnumMap, v),
    ),
    stops: $checkedConvert(
      'stops',
      (v) => (v as List<dynamic>)
          .map((e) => DriverStopResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    timezone: $checkedConvert('timezone', (v) => v as String),
  );
  return val;
});

Map<String, dynamic> _$DriverRouteDetailResponseToJson(
  DriverRouteDetailResponse instance,
) => <String, dynamic>{
  'end': instance.end.toJson(),
  'planDate': instance.planDate.toIso8601String(),
  'planId': instance.planId,
  'planName': instance.planName,
  'routeId': instance.routeId,
  'start': instance.start.toJson(),
  'status': _$DriverRouteDetailResponseStatusEnumEnumMap[instance.status]!,
  'stops': instance.stops.map((e) => e.toJson()).toList(),
  'timezone': instance.timezone,
};

const _$DriverRouteDetailResponseStatusEnumEnumMap = {
  DriverRouteDetailResponseStatusEnum.PENDING: 'PENDING',
  DriverRouteDetailResponseStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  DriverRouteDetailResponseStatusEnum.COMPLETED: 'COMPLETED',
};
