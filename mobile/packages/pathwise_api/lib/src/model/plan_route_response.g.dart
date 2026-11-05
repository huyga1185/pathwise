// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_route_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanRouteResponse _$PlanRouteResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanRouteResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'driver',
          'end',
          'id',
          'load',
          'maxDistanceMeters',
          'plannedDistanceMeters',
          'plannedDurationSeconds',
          'plannedEnd',
          'plannedStart',
          'shiftEnd',
          'shiftStart',
          'start',
          'status',
          'stops',
          'vehicle',
        ],
      );
      final val = PlanRouteResponse(
        driver: $checkedConvert(
          'driver',
          (v) => Driver.fromJson(v as Map<String, dynamic>),
        ),
        end: $checkedConvert(
          'end',
          (v) => LocationResponse.fromJson(v as Map<String, dynamic>),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        load: $checkedConvert('load', (v) => (v as num?)?.toInt()),
        maxDistanceMeters: $checkedConvert(
          'maxDistanceMeters',
          (v) => (v as num?)?.toInt(),
        ),
        plannedDistanceMeters: $checkedConvert(
          'plannedDistanceMeters',
          (v) => (v as num?)?.toInt(),
        ),
        plannedDurationSeconds: $checkedConvert(
          'plannedDurationSeconds',
          (v) => (v as num?)?.toInt(),
        ),
        plannedEnd: $checkedConvert(
          'plannedEnd',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        plannedStart: $checkedConvert(
          'plannedStart',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        shiftEnd: $checkedConvert('shiftEnd', (v) => v as String),
        shiftStart: $checkedConvert('shiftStart', (v) => v as String),
        start: $checkedConvert(
          'start',
          (v) => LocationResponse.fromJson(v as Map<String, dynamic>),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$PlanRouteResponseStatusEnumEnumMap, v),
        ),
        stops: $checkedConvert(
          'stops',
          (v) => (v as List<dynamic>)
              .map((e) => PlanStopResponse.fromJson(e as Map<String, dynamic>))
              .toList(),
        ),
        vehicle: $checkedConvert(
          'vehicle',
          (v) => Vehicle.fromJson(v as Map<String, dynamic>),
        ),
      );
      return val;
    });

Map<String, dynamic> _$PlanRouteResponseToJson(PlanRouteResponse instance) =>
    <String, dynamic>{
      'driver': instance.driver.toJson(),
      'end': instance.end.toJson(),
      'id': instance.id,
      'load': instance.load,
      'maxDistanceMeters': instance.maxDistanceMeters,
      'plannedDistanceMeters': instance.plannedDistanceMeters,
      'plannedDurationSeconds': instance.plannedDurationSeconds,
      'plannedEnd': instance.plannedEnd?.toIso8601String(),
      'plannedStart': instance.plannedStart?.toIso8601String(),
      'shiftEnd': instance.shiftEnd,
      'shiftStart': instance.shiftStart,
      'start': instance.start.toJson(),
      'status': _$PlanRouteResponseStatusEnumEnumMap[instance.status]!,
      'stops': instance.stops.map((e) => e.toJson()).toList(),
      'vehicle': instance.vehicle.toJson(),
    };

const _$PlanRouteResponseStatusEnumEnumMap = {
  PlanRouteResponseStatusEnum.PENDING: 'PENDING',
  PlanRouteResponseStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  PlanRouteResponseStatusEnum.COMPLETED: 'COMPLETED',
};
