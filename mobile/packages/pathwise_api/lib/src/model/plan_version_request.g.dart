// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'plan_version_request.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PlanVersionRequest _$PlanVersionRequestFromJson(Map<String, dynamic> json) =>
    $checkedCreate('PlanVersionRequest', json, ($checkedConvert) {
      $checkKeys(json, requiredKeys: const ['version']);
      final val = PlanVersionRequest(
        version: $checkedConvert('version', (v) => (v as num).toInt()),
      );
      return val;
    });

Map<String, dynamic> _$PlanVersionRequestToJson(PlanVersionRequest instance) =>
    <String, dynamic>{'version': instance.version};
