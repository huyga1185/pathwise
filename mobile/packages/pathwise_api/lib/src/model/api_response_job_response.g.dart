// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_job_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseJobResponse _$ApiResponseJobResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseJobResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseJobResponse(
    data: $checkedConvert(
      'data',
      (v) => JobResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseJobResponseToJson(
  ApiResponseJobResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
