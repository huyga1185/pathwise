// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_plan_copy_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePlanCopyResponse _$ApiResponsePlanCopyResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePlanCopyResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePlanCopyResponse(
    data: $checkedConvert(
      'data',
      (v) => PlanCopyResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePlanCopyResponseToJson(
  ApiResponsePlanCopyResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
