// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_plan_import_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponsePlanImportResponse _$ApiResponsePlanImportResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponsePlanImportResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponsePlanImportResponse(
    data: $checkedConvert(
      'data',
      (v) => PlanImportResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponsePlanImportResponseToJson(
  ApiResponsePlanImportResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
