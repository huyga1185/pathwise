// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'driver_stop_update_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

DriverStopUpdateResponse _$DriverStopUpdateResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('DriverStopUpdateResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['planStatus', 'routeStatus', 'stop']);
  final val = DriverStopUpdateResponse(
    planStatus: $checkedConvert(
      'planStatus',
      (v) => $enumDecode(_$DriverStopUpdateResponsePlanStatusEnumEnumMap, v),
    ),
    routeStatus: $checkedConvert(
      'routeStatus',
      (v) => $enumDecode(_$DriverStopUpdateResponseRouteStatusEnumEnumMap, v),
    ),
    stop: $checkedConvert(
      'stop',
      (v) => DriverStopResponse.fromJson(v as Map<String, dynamic>),
    ),
  );
  return val;
});

Map<String, dynamic> _$DriverStopUpdateResponseToJson(
  DriverStopUpdateResponse instance,
) => <String, dynamic>{
  'planStatus':
      _$DriverStopUpdateResponsePlanStatusEnumEnumMap[instance.planStatus]!,
  'routeStatus':
      _$DriverStopUpdateResponseRouteStatusEnumEnumMap[instance.routeStatus]!,
  'stop': instance.stop.toJson(),
};

const _$DriverStopUpdateResponsePlanStatusEnumEnumMap = {
  DriverStopUpdateResponsePlanStatusEnum.DRAFT: 'DRAFT',
  DriverStopUpdateResponsePlanStatusEnum.PUBLISHED: 'PUBLISHED',
  DriverStopUpdateResponsePlanStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  DriverStopUpdateResponsePlanStatusEnum.COMPLETED: 'COMPLETED',
  DriverStopUpdateResponsePlanStatusEnum.CANCELLED: 'CANCELLED',
};

const _$DriverStopUpdateResponseRouteStatusEnumEnumMap = {
  DriverStopUpdateResponseRouteStatusEnum.PENDING: 'PENDING',
  DriverStopUpdateResponseRouteStatusEnum.IN_PROGRESS: 'IN_PROGRESS',
  DriverStopUpdateResponseRouteStatusEnum.COMPLETED: 'COMPLETED',
};
