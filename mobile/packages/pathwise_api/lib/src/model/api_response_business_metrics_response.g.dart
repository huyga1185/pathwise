// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_business_metrics_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseBusinessMetricsResponse _$ApiResponseBusinessMetricsResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseBusinessMetricsResponse', json, (
  $checkedConvert,
) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseBusinessMetricsResponse(
    data: $checkedConvert(
      'data',
      (v) => BusinessMetricsResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseBusinessMetricsResponseToJson(
  ApiResponseBusinessMetricsResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
