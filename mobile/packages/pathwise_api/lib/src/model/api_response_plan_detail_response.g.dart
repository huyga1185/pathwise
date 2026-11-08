// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_plan_detail_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePlanDetailResponse _$ApiResponsePlanDetailResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePlanDetailResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePlanDetailResponse(
    data: $checkedConvert(
      'data',
      (v) => PlanDetailResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePlanDetailResponseToJson(
  ApiResponsePlanDetailResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
