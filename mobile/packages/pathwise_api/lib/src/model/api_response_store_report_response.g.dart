// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'api_response_store_report_response.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

ApiResponseStoreReportResponse _$ApiResponseStoreReportResponseFromJson(
  Map<String, dynamic> json,
) => $checkedCreate('ApiResponseStoreReportResponse', json, ($checkedConvert) {
  $checkKeys(json, requiredKeys: const ['data']);
  final val = ApiResponseStoreReportResponse(
    data: $checkedConvert(
      'data',
      (v) => StoreReportResponse.fromJson(v as Map<String, dynamic>),
    ),
    message: $checkedConvert('message', (v) => v as String?),
  );
  return val;
});

Map<String, dynamic> _$ApiResponseStoreReportResponseToJson(
  ApiResponseStoreReportResponse instance,
) => <String, dynamic>{
  'data': instance.data.toJson(),
  'message': ?instance.message,
};
