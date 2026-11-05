// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_page_response_plan_summary_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePageResponsePlanSummaryResponse
_$ApiResponsePageResponsePlanSummaryResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePageResponsePlanSummaryResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePageResponsePlanSummaryResponse(
    data: $checkedConvert(
      'data',
      (v) =>
          PageResponsePlanSummaryResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePageResponsePlanSummaryResponseToJson(
  ApiResponsePageResponsePlanSummaryResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
