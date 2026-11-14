// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_technical_metrics_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseTechnicalMetricsResponse
_$ApiResponseTechnicalMetricsResponseFromJson(Map<String, dynamic> json) =>
    $checkedCreate('ApiResponseTechnicalMetricsResponse', json, (
      $checkedConvert,
    ) {
      $checkKeys(json, requiredKeys: const ['data']);
      final val = ApiResponseTechnicalMetricsResponse(
        data: $checkedConvert(
          'data',
          (v) => TechnicalMetricsResponse.fromJson(v as Map<String, dynamic>),
        ),
        message: $checkedConvert('message', (v) => v as String?),
      );
      return val;
    });

Map<String, dynamic> _$ApiResponseTechnicalMetricsResponseToJson(
  ApiResponseTechnicalMetricsResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
