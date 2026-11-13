// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'violation_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ViolationResponse _$ViolationResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ViolationResponse', json, ($checkedConvert) {
      $checkKeys(
        json,
        requiredKeys: const ['code', 'excess', 'routeId', 'stopId'],
      );
      final val = ViolationResponse(
        code: $checkedConvert(
          'code',
          (v) => $enumDecode(_$ViolationResponseCodeEnumEnumMap, v),
        ),
        excess: $checkedConvert('excess', (v) => (v as num).toInt()),
        routeId: $checkedConvert('routeId', (v) => (v as num).toInt()),
        stopId: $checkedConvert('stopId', (v) => (v as num?)?.toInt()),
      );
      return val;
    });

Map<String, dynamic> _$ViolationResponseToJson(ViolationResponse instance) =>
    <String, dynamic>{
      'code': _$ViolationResponseCodeEnumEnumMap[instance.code]!,
      'excess': instance.excess,
      'routeId': instance.routeId,
      'stopId': instance.stopId,
    };

const _$ViolationResponseCodeEnumEnumMap = {
  ViolationResponseCodeEnum.CAPACITY_EXCEEDED: 'CAPACITY_EXCEEDED',
  ViolationResponseCodeEnum.TIME_WINDOW_MISSED: 'TIME_WINDOW_MISSED',
  ViolationResponseCodeEnum.SHIFT_EXCEEDED: 'SHIFT_EXCEEDED',
  ViolationResponseCodeEnum.MAX_DISTANCE_EXCEEDED: 'MAX_DISTANCE_EXCEEDED',
};
