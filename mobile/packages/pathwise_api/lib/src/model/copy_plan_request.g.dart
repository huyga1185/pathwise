// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'copy_plan_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

CopyPlanRequest _$CopyPlanRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('CopyPlanRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['planDate', 'stops']);
      final val = CopyPlanRequest(
        name: $checkedConvert('name', (v) => v as String?),
        planDate: $checkedConvert('planDate', (v) => v as String),
        stops: $checkedConvert(
          'stops',
          (v) => $enumDecode(_$CopyPlanRequestStopsEnumEnumMap, v),
        ),
      );
      return val;
    });

Map<String, dynamic> _$CopyPlanRequestToJson(CopyPlanRequest instance) =>
    <String, dynamic>{
      'name': ?instance.name,
      'planDate': instance.planDate,
      'stops': _$CopyPlanRequestStopsEnumEnumMap[instance.stops]!,
    };

const _$CopyPlanRequestStopsEnumEnumMap = {
  CopyPlanRequestStopsEnum.ALL: 'ALL',
  CopyPlanRequestStopsEnum.NOT_DELIVERED: 'NOT_DELIVERED',
};
