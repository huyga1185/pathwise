// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'update_stop_status_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

UpdateStopStatusRequest _$UpdateStopStatusRequestFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('UpdateStopStatusRequest', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['status']);
  final val = UpdateStopStatusRequest(
    failureReason: $checkedConvert('failureReason', (v) => v as String?),
    status: $checkedConvert(
      'status',
      (v) => $enumDecode(_$UpdateStopStatusRequestStatusEnumEnumMap, v),
    ),
  );
  return val;
});

Map<String, dynamic> _$UpdateStopStatusRequestToJson(
  UpdateStopStatusRequest instance,
) => <String, dynamic>{
  'failureReason': ?instance.failureReason,
  'status': _$UpdateStopStatusRequestStatusEnumEnumMap[instance.status]!,
};

const _$UpdateStopStatusRequestStatusEnumEnumMap = {
  UpdateStopStatusRequestStatusEnum.PENDING: 'PENDING',
  UpdateStopStatusRequestStatusEnum.DELIVERED: 'DELIVERED',
  UpdateStopStatusRequestStatusEnum.FAILED: 'FAILED',
  UpdateStopStatusRequestStatusEnum.SKIPPED: 'SKIPPED',
};
