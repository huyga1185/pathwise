// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanDetailResponse _$PlanDetailResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('PlanDetailResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'activeJob',
      'depot',
      'geocodingSummary',
      'id',
      'name',
      'optimizedAt',
      'planDate',
      'processingStatus',
      'routes',
      'status',
      'timezone',
      'totalDistanceMeters',
      'totalDurationSeconds',
      'unassignedStops',
      'version',
    ],
  );
  final val = PlanDetailResponse(
    activeJob: $checkedConvert(
      'activeJob',
      (v) => v == null ? null : JobResponse.fromJson(v as Map<String, dynamic>),
    ),
    depot: $checkedConvert(
      'depot',
      (v) => LocationResponse.fromJson(v as Map<String, dynamic>),
    ),
    geocodingSummary: $checkedConvert(
      'geocodingSummary',
      (v) => GeocodingSummaryResponse.fromJson(v as Map<String, dynamic>),
    ),
    id: $checkedConvert('id', (v) => (v as num).toInt()),
    name: $checkedConvert('name', (v) => v as String),
    optimizedAt: $checkedConvert(
      'optimizedAt',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    planDate: $checkedConvert('planDate', (v) => DateTime.parse(v as String)),
    processingStatus: $checkedConvert(
      'processingStatus',
      (v) => $enumDecode(_$PlanDetailResponseProcessingStatusEnumEnumMap, v),
    ),
    routes: $checkedConvert(
      'routes',
      (v) => (v as List<dynamic>)
          .map((e) => PlanRouteResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$PlanDetailResponseStatusEnumEnumMap, v),
    ),
    timezone: $checkedConvert('timezone', (v) => v as String),
    totalDistanceMeters: $checkedConvert(
      'totalDistanceMeters',
      (v) => (v as num?)?.toInt(),
    ),
    totalDurationSeconds: $checkedConvert(
      'totalDurationSeconds',
      (v) => (v as num?)?.toInt(),
    ),
    unassignedStops: $checkedConvert(
      'unassignedStops',
      (v) => (v as List<dynamic>)
          .map((e) => PlanStopResponse.fromJson(e as Map<String, dynamic>))
          .toList(),
    ),
    version: $checkedConvert('version', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$PlanDetailResponseToJson(
  PlanDetailResponse instance,
) => <String, dynamic>{
  'activeJob': instance.activeJob?.toJson(),
  'depot': instance.depot.toJson(),
  'geocodingSummary': instance.geocodingSummary.toJson(),
  'id': instance.id,
  'name': instance.name,
  'optimizedAt': instance.optimizedAt?.toIso8601String(),
  'planDate': instance.planDate.toIso8601String(),
  'processingStatus':
      _$PlanDetailResponseProcessingStatusEnumEnumMap[instance
          .processingStatus]!,
  'routes': instance.routes.map((e) => e.toJson()).toList(),
  'status': _$PlanDetailResponseStatusEnumEnumMap[instance.status]!,
  'timezone': instance.timezone,
  'totalDistanceMeters': instance.totalDistanceMeters,
  'totalDurationSeconds': instance.totalDurationSeconds,
  'unassignedStops': instance.unassignedStops.map((e) => e.toJson()).toList(),
  'version': instance.version,
};

const _$PlanDetailResponseProcessingStatusEnumEnumMap = {
  PlanDetailResponseProcessingStatusEnum.IDLE: 'IDLE',
  PlanDetailResponseProcessingStatusEnum.GEOCODING: 'GEOCODING',
  PlanDetailResponseProcessingStatusEnum.OPTIMIZING: 'OPTIMIZING',
};

const _$PlanDetailResponseStatusEnumEnumMap = {
  PlanDetailResponseStatusEnum.DRAFT: 'DRAFT',
  PlanDetailResponseStatusEnum.PUBLISHED: 'PUBLISHED',
  PlanDetailResponseStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  PlanDetailResponseStatusEnum.COMPLETED: 'COMPLETED',
  PlanDetailResponseStatusEnum.CANCELLED: 'CANCELLED',
};
