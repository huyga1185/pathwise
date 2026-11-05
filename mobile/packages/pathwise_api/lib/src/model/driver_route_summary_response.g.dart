// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_route_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverRouteSummaryResponse _$DriverRouteSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DriverRouteSummaryResponse', json, ($checkedConvert) {
  $checkKeys(
    json,
    requiredKeys: const [
      'completedCount',
      'planDate',
      'planId',
      'planName',
      'plannedStart',
      'routeId',
      'status',
      'stopCount',
    ],
  );
  final val = DriverRouteSummaryResponse(
    completedCount: $checkedConvert(
      'completedCount',
      (v) => (v as num).toInt(),
    ),
    planDate: $checkedConvert('planDate', (v) => DateTime.parse(v as String)),
    planId: $checkedConvert('planId', (v) => (v as num).toInt()),
    planName: $checkedConvert('planName', (v) => v as String),
    plannedStart: $checkedConvert(
      'plannedStart',
      (v) => v == null ? null : DateTime.parse(v as String),
    ),
    routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$DriverRouteSummaryResponseStatusEnumEnumMap, v),
    ),
    stopCount: $checkedConvert('stopCount', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$DriverRouteSummaryResponseToJson(
  DriverRouteSummaryResponse instance,
) => <String, dynamic>{
  'completedCount': instance.completedCount,
  'planDate': instance.planDate.toIso8601String(),
  'planId': instance.planId,
  'planName': instance.planName,
  'plannedStart': instance.plannedStart?.toIso8601String(),
  'routeId': instance.routeId,
  'status': _$DriverRouteSummaryResponseStatusEnumEnumMap[instance.status]!,
  'stopCount': instance.stopCount,
};

const _$DriverRouteSummaryResponseStatusEnumEnumMap = {
  DriverRouteSummaryResponseStatusEnum.PENDING: 'PENDING',
  DriverRouteSummaryResponseStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  DriverRouteSummaryResponseStatusEnum.COMPLETED: 'COMPLETED',
};
