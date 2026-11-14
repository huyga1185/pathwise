// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'skipped_assignment_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

SkippedAssignmentResponse _$SkippedAssignmentResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('SkippedAssignmentResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['driverId', 'reason', 'vehicleId']);
  final val = SkippedAssignmentResponse(
    driverId: $checkedConvert('driverId', (v) => (v as num).toInt()),
    reason: $checkedConvert(
      'reason',
      (v) => $enumDecode(_$SkippedAssignmentResponseReasonEnumEnumMap, v),
    ),
    vehicleId: $checkedConvert('vehicleId', (v) => (v as num).toInt()),
  );
  return val;
});

Map<String, dynamic> _$SkippedAssignmentResponseToJson(
  SkippedAssignmentResponse instance,
) => <String, dynamic>{
  'driverId': instance.driverId,
  'reason': _$SkippedAssignmentResponseReasonEnumEnumMap[instance.reason]!,
  'vehicleId': instance.vehicleId,
};

const _$SkippedAssignmentResponseReasonEnumEnumMap = {
  SkippedAssignmentResponseReasonEnum.DRIVER_UNAVAILABLE: 'DRIVER_UNAVAILABLE',
  SkippedAssignmentResponseReasonEnum.VEHICLE_INACTIVE: 'VEHICLE_INACTIVE',
};
