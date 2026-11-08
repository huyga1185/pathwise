// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanSummaryResponse _$PlanSummaryResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanSummaryResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'createdAt',
          'id',
          'name',
          'optimizedAt',
          'planDate',
          'processingStatus',
          'routeCount',
          'status',
          'stopCount',
          'totalDistanceMeters',
          'totalDurationSeconds',
          'version',
        ],
      );
      final val = PlanSummaryResponse(
        createdAt: $checkedConvert(
          'createdAt',
          (v) => DateTime.parse(v as String),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        name: $checkedConvert('name', (v) => v as String),
        optimizedAt: $checkedConvert(
          'optimizedAt',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        planDate: $checkedConvert('planDate', (v) => v as String),
        processingStatus: $checkedConvert(
          'processingStatus',
          (v) =>
              $enumDecode(_$PlanSummaryResponseProcessingStatusEnumEnumMap, v),
        ),
        routeCount: $checkedConvert('routeCount', (v) => (v as num).toInt()),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$PlanSummaryResponseStatusEnumEnumMap, v),
        ),
        stopCount: $checkedConvert('stopCount', (v) => (v as num).toInt()),
        totalDistanceMeters: $checkedConvert(
          'totalDistanceMeters',
          (v) => (v as num?)?.toInt(),
        ),
        totalDurationSeconds: $checkedConvert(
          'totalDurationSeconds',
          (v) => (v as num?)?.toInt(),
        ),
        version: $checkedConvert('version', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$PlanSummaryResponseToJson(
  PlanSummaryResponse instance,
) => <String, dynamic>{
  'createdAt': instance.createdAt.toIso8601String(),
  'id': instance.id,
  'name': instance.name,
  'optimizedAt': instance.optimizedAt?.toIso8601String(),
  'planDate': instance.planDate,
  'processingStatus':
      _$PlanSummaryResponseProcessingStatusEnumEnumMap[instance
          .processingStatus]!,
  'routeCount': instance.routeCount,
  'status': _$PlanSummaryResponseStatusEnumEnumMap[instance.status]!,
  'stopCount': instance.stopCount,
  'totalDistanceMeters': instance.totalDistanceMeters,
  'totalDurationSeconds': instance.totalDurationSeconds,
  'version': instance.version,
};

const _$PlanSummaryResponseProcessingStatusEnumEnumMap = {
  PlanSummaryResponseProcessingStatusEnum.IDLE: 'IDLE',
  PlanSummaryResponseProcessingStatusEnum.GEOCODING: 'GEOCODING',
  PlanSummaryResponseProcessingStatusEnum.OPTIMIZING: 'OPTIMIZING',
};

const _$PlanSummaryResponseStatusEnumEnumMap = {
  PlanSummaryResponseStatusEnum.DRAFT: 'DRAFT',
  PlanSummaryResponseStatusEnum.PUBLISHED: 'PUBLISHED',
  PlanSummaryResponseStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  PlanSummaryResponseStatusEnum.COMPLETED: 'COMPLETED',
  PlanSummaryResponseStatusEnum.CANCELLED: 'CANCELLED',
};
