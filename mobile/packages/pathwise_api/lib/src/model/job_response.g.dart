// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'job_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

JobResponse _$JobResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('JobResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const [
          'createdAt',
          'errorCode',
          'finishedAt',
          'id',
          'planId',
          'startedAt',
          'status',
          'type',
        ],
      );
      final val = JobResponse(
        createdAt: $checkedConvert(
          'createdAt',
          (v) => DateTime.parse(v as String),
        ),
        errorCode: $checkedConvert('errorCode', (v) => v as String?),
        finishedAt: $checkedConvert(
          'finishedAt',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        id: $checkedConvert('id', (v) => (v as num).toInt()),
        planId: $checkedConvert('planId', (v) => (v as num).toInt()),
        startedAt: $checkedConvert(
          'startedAt',
          (v) => v == null ? null : DateTime.parse(v as String),
        ),
        status: $checkedConvert(
          'status',
          (v) => $enumDecode(_$JobResponseStatusEnumEnumMap, v),
        ),
        type: $checkedConvert(
          'type',
          (v) => $enumDecode(_$JobResponseTypeEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$JobResponseToJson(JobResponse instance) =>
    <String, dynamic>{
      'createdAt': instance.createdAt.toIso8601String(),
      'errorCode': instance.errorCode,
      'finishedAt': instance.finishedAt?.toIso8601String(),
      'id': instance.id,
      'planId': instance.planId,
      'startedAt': instance.startedAt?.toIso8601String(),
      'status': _$JobResponseStatusEnumEnumMap[instance.status]!,
      'type': _$JobResponseTypeEnumEnumMap[instance.type]!,
    };

const _$JobResponseStatusEnumEnumMap = {
  JobResponseStatusEnum.QUEUED: 'QUEUED',
  JobResponseStatusEnum.RUNNING: 'RUNNING',
  JobResponseStatusEnum.SUCCEEDED: 'SUCCEEDED',
  JobResponseStatusEnum.FAILED: 'FAILED',
};

const _$JobResponseTypeEnumEnumMap = {
  JobResponseTypeEnum.GEOCODE_PLAN: 'GEOCODE_PLAN',
  JobResponseTypeEnum.OPTIMIZE_PLAN: 'OPTIMIZE_PLAN',
};
